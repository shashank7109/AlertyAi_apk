package com.alertyai.app.network

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.*
import java.util.concurrent.TimeUnit

data class UserWsMessage(
    val type: String,
    val task: BackendTask? = null
)

class UserWebSocketManager(private val gson: Gson = Gson()) {

    private val TAG = "UserWebSocketManager"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    var isConnected: Boolean = false; private set

    private var onMessageCallback: ((UserWsMessage) -> Unit)? = null
    private var currentUserId: String? = null
    private var currentToken: String? = null

    private var isReconnecting = false
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect(
        userId: String,
        token: String,
        onMessage: (UserWsMessage) -> Unit
    ) {
        onMessageCallback = onMessage
        currentUserId = userId
        currentToken = token
        connectInternal()
    }

    private fun connectInternal() {
        if (currentUserId == null || currentToken == null) return
        val userId = currentUserId!!
        val token = currentToken!!

        val base = RetrofitClient.BASE_URL.trimEnd('/')
        val wsBase = base.replace("https://", "wss://").replace("http://", "ws://")
        val wsUrl = "$wsBase/ws/user/$userId?token=$token"

        Log.d(TAG, "Connecting User WS to: $wsUrl")

        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                isReconnecting = false
                reconnectJob?.cancel()
                Log.d(TAG, "✅ User WebSocket Connected | User: $userId")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d(TAG, "📥 User WS Received: $text")
                    val msg = gson.fromJson(text, UserWsMessage::class.java)
                    onMessageCallback?.invoke(msg)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing User WS message: $text", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e(TAG, "❌ User WS Failure: ${t.message} | HTTP: ${response?.code}")
                tryReconnect()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                tryReconnect()
            }
        })
    }

    private fun tryReconnect() {
        if (isReconnecting || currentUserId == null) return
        isReconnecting = true
        Log.d(TAG, "Attempting User WS reconnect in 3 seconds...")
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(3000)
            isReconnecting = false
            connectInternal()
        }
    }

    fun disconnect() {
        isConnected = false
        isReconnecting = false
        reconnectJob?.cancel()
        currentUserId = null
        currentToken = null
        webSocket?.close(1000, "App backgrounded")
        webSocket = null
        Log.d(TAG, "User WS disconnected")
    }
}
