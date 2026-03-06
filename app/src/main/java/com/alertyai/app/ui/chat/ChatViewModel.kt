package com.alertyai.app.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertyai.app.data.model.CheckItem
import com.alertyai.app.data.model.Priority
import com.alertyai.app.data.model.Task
import com.alertyai.app.data.repository.TaskRepository
import com.alertyai.app.network.RetrofitClient
import com.alertyai.app.network.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.util.Log

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
    val error: String? = null,
    val replyingTo: ChatMessage? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    fun setReplyingTo(msg: ChatMessage?) {
        _state.value = _state.value.copy(replyingTo = msg)
    }

    fun sendMessage(context: Context, text: String) {
        if (text.isBlank()) return
        val token = TokenManager.getToken(context) ?: run {
            _state.value = _state.value.copy(error = "Please log in first")
            return
        }

        val replyTo = _state.value.replyingTo
        val finalContent = if (replyTo != null) {
            "> ${replyTo.content.take(80).replace("\n", " ")}...\n\n$text"
        } else text

        // Add user message instantly
        val userMsg = ChatMessage(role = MessageRole.USER, content = finalContent)
        _state.value = _state.value.copy(
            messages = _state.value.messages + userMsg,
            isLoading = true,
            error = null,
            replyingTo = null
        )

        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.chat(
                    bearer = "Bearer $token",
                    message = text.trim()
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d("ChatViewModel", "Response body: $body")
                    
                    // If a task was created, save it to local database
                    if (body.taskCreated && body.task != null) {
                        Log.d("ChatViewModel", "Parsing created task from AI response")
                        try {
                            val taskMap = body.task
                            val taskTitle = (taskMap["title"] as? String) ?: text.trim()
                            val taskId = (taskMap["_id"] as? String).orEmpty()
                            
                            // Parse task data similar to TasksViewModel
                            val checklistJson = com.google.gson.Gson().toJson(
                                ((taskMap["subtasks"] as? List<*>)?.filterIsInstance<String>() ?: emptyList())
                                    .map { CheckItem(text = it) }
                            )
                            val note = (taskMap["description"] as? String)
                                ?: (taskMap["note"] as? String)
                                ?: "Created by AI"
                            val dueDateStr = taskMap["due_date"] as? String
                            val dueTimeStr = (taskMap["due_time"] as? String) ?: (taskMap["time"] as? String)
                            val priorityStr = taskMap["priority"] as? String
                            
                            val dueTimeMillis = parseTimeToMillis(dueTimeStr) ?: run {
                                val isoTime = dueDateStr?.takeIf { it.contains("T") }
                                    ?.substringAfter("T")?.take(5)
                                parseTimeToMillis(isoTime?.takeIf { it != "00:00" })
                            }
                            
                            // Save task to local Room database
                            taskRepository.addTask(Task(
                                title = taskTitle,
                                note = note,
                                checklistJson = checklistJson,
                                dueDate = parseDateToMillis(dueDateStr),
                                dueTime = dueTimeMillis,
                                priority = parsePriority(priorityStr),
                                backendId = taskId
                            ))
                            Log.d("ChatViewModel", "Task saved successfully: $taskTitle")
                        } catch(e: Exception) {
                            Log.e("ChatViewModel", "Error saving AI task to local DB", e)
                        }
                    } else {
                        Log.d("ChatViewModel", "Task creation skipped. taskCreated: ${body.taskCreated}, task: ${body.task}")
                    }
                    
                    val aiMsg = ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = body.reply.ifBlank { "I'm here to help!" },
                        taskCreated = body.taskCreated,
                        taskTitle = (body.task?.get("title") as? String)
                    )
                    _state.value = _state.value.copy(
                        messages = _state.value.messages + aiMsg,
                        isLoading = false,
                        error = null
                    )
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Session expired. Please log in again."
                        500 -> "Server error. Please try again."
                        else -> "Failed to send message (${response.code()})"
                    }
                    appendError(errorMsg)
                }
            } catch (e: Exception) {
                // Fallback offline response
                val fallback = generateOfflineReply(text)
                val aiMsg = ChatMessage(role = MessageRole.ASSISTANT, content = fallback)
                _state.value = _state.value.copy(
                    messages = _state.value.messages + aiMsg,
                    isLoading = false,
                    error = null
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
    
    private fun parseDateToMillis(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            if (dateStr.contains("T")) {
                val clean = dateStr.substringBefore(".")
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(clean)?.time
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)?.time
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTimeToMillis(timeStr: String?): Long? {
        if (timeStr.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("HH:mm", Locale.getDefault()).parse(timeStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePriority(priorityStr: String?): Priority {
        return when (priorityStr?.lowercase()) {
            "high" -> Priority.HIGH
            "low" -> Priority.LOW
            else -> Priority.NORMAL
        }
    }
}
