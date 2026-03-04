package com.alertyai.app.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertyai.app.network.RetrofitClient
import com.alertyai.app.network.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

enum class MessageRole { USER, ASSISTANT }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val taskCreated: Boolean = false,
    val taskTitle: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "Hi! I'm your AI assistant 🤖\n\nI can help you manage tasks, create weekly plans, and answer questions about your productivity.\n\nTry: \"Remind me to call mom tomorrow\" or \"Create a task to finish the report by Friday\"."
        )
    ),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatViewModel : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    fun sendMessage(context: Context, text: String) {
        if (text.isBlank()) return
        val token = TokenManager.getToken(context) ?: return

        // Add user message instantly
        val userMsg = ChatMessage(role = MessageRole.USER, content = text.trim())
        _state.value = _state.value.copy(
            messages = _state.value.messages + userMsg,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.chat(
                    bearer = "Bearer $token",
                    message = text.trim()
                )
                if (response.isSuccessful) {
                    val body = response.body()!!
                    val aiMsg = ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = body.reply.ifBlank { "I'm here to help!" },
                        taskCreated = body.taskCreated,
                        taskTitle = (body.task?.get("title") as? String)
                    )
                    _state.value = _state.value.copy(
                        messages = _state.value.messages + aiMsg,
                        isLoading = false
                    )
                } else {
                    appendError("Server error ${response.code()}")
                }
            } catch (e: Exception) {
                // Fallback offline response
                val fallback = generateOfflineReply(text)
                val aiMsg = ChatMessage(role = MessageRole.ASSISTANT, content = fallback)
                _state.value = _state.value.copy(
                    messages = _state.value.messages + aiMsg,
                    isLoading = false
                )
            }
        }
    }

    private fun appendError(msg: String) {
        _state.value = _state.value.copy(isLoading = false, error = msg)
    }

    private fun generateOfflineReply(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("task") || q.contains("todo") ->
                "I can help with tasks! Use the Tasks tab to add tasks with AI, image, or voice. 📋"
            q.contains("remind") ->
                "Set reminders in the Reminders tab — I'll notify you on time! ⏰"
            q.contains("plan") || q.contains("week") ->
                "For weekly planning, add your tasks with due dates and I'll help prioritize. 📅"
            q.contains("help") ->
                "I can help with: task management, reminders, OCR from images, voice tasks. Just ask!"
            else ->
                "Got it! You can also create tasks from this chat by saying something like \"I need to call John tomorrow\"."
        }
    }
}
