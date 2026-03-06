package com.alertyai.app.network

import android.util.Log
import com.alertyai.app.data.model.TeamChatMessage
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.*
import java.util.concurrent.TimeUnit

class WebSocketManager(private val gson: Gson = Gson()) {

    private val TAG = "WebSocketManager"

    // Dedicated client with no read timeout — required for persistent WebSocket connections
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS) // keep-alive ping every 30s
        .build()

    private var webSocket: WebSocket? = null

    // Connection state
    var isConnected: Boolean = false
        private set

    private var onMessageCallback: ((TeamChatMessage) -> Unit)? = null
    private var onErrorCallback: ((Throwable) -> Unit)? = null
    private var onConnectedCallback: (() -> Unit)? = null
    private var currentTeamId: String? = null
    private var currentToken: String? = null

    private var isReconnecting = false
    private var reconnectJob: Job? = null
    // Use a coroutine scope for delaying reconnect attempts
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect(
        teamId: String,
        token: String,
        onConnected: () -> Unit = {},
        onMessage: (TeamChatMessage) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        onMessageCallback = onMessage
        onErrorCallback = onError
        onConnectedCallback = onConnected
        currentTeamId = teamId
        currentToken = token

        connectInternal()
    }

    private fun connectInternal() {
        if (currentTeamId == null || currentToken == null) return
        val teamId = currentTeamId!!
        val token = currentToken!!

        // Fix: base URL is https://x.com/ → ws URL should be wss://x.com/api/chat/ws/chat/{teamId}
        val base = RetrofitClient.BASE_URL.trimEnd('/')
        val wsBase = base.replace("https://", "wss://").replace("http://", "ws://")
        val wsUrl = "$wsBase/api/chat/ws/chat/$teamId?token=$token"

        Log.d(TAG, "Connecting to: $wsUrl")

        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                isReconnecting = false
                reconnectJob?.cancel()
                Log.d(TAG, "✅ WebSocket Connected | Team: $teamId")
                onConnectedCallback?.invoke()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d(TAG, "📥 Received: $text")
                    val msg = gson.fromJson(text, TeamChatMessage::class.java)
                    onMessageCallback?.invoke(msg)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message: $text", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e(TAG, "❌ WebSocket Failure: ${t.message} | HTTP: ${response?.code}")
                onErrorCallback?.invoke(t)
                tryReconnect()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d(TAG, "WebSocket Closing [$code]: $reason")
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d(TAG, "WebSocket Closed [$code]: $reason")
                tryReconnect()
            }
        })
    }

    private fun tryReconnect() {
        if (isReconnecting || currentTeamId == null) return
        isReconnecting = true
        Log.d(TAG, "Attempting to reconnect in 3 seconds...")
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(3000)
            isReconnecting = false
            connectInternal()
        }
    }

    fun sendMessage(text: String): Boolean {
        if (!isConnected || webSocket == null) {
            Log.w(TAG, "Cannot send — not connected")
            return false
        }
        val payload = mapOf("text" to text)
        val sent = webSocket!!.send(gson.toJson(payload))
        Log.d(TAG, "📤 Sent: $text | success=$sent")
        return sent
    }

    fun disconnect() {
        isConnected = false
        isReconnecting = false
        reconnectJob?.cancel()
        currentTeamId = null
        currentToken = null
        webSocket?.close(1000, "Screen closed")
        webSocket = null
        Log.d(TAG, "WebSocket disconnected")
    }
}
