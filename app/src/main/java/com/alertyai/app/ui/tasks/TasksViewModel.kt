package com.alertyai.app.ui.tasks

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertyai.app.data.model.Priority
import com.alertyai.app.data.model.Task
import com.alertyai.app.data.repository.TaskRepository
import com.alertyai.app.network.RetrofitClient
import com.alertyai.app.network.TokenManager
import com.alertyai.app.util.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class AiOpState(
    val isAiLoading: Boolean = false,
    val aiError: String? = null,
    val aiSuccess: String? = null
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    // Expose tasks as StateFlow — HomeScreen uses `tasksVm.tasks`
    val tasks: StateFlow<List<Task>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _aiState = MutableStateFlow(AiOpState())
    val aiState: StateFlow<AiOpState> = _aiState.asStateFlow()

    fun addTask(title: String, note: String = "", priority: Priority = Priority.NORMAL) {
        viewModelScope.launch {
            repository.addTask(Task(title = title, note = note, priority = priority))
        }
    }

    /** Save a fully-configured Task and schedule its alarm if enabled. */
    fun addFullTask(context: Context, task: Task) {
        viewModelScope.launch {
            val inserted = repository.addTask(task)
            // If the task got a generated ID, we need to fetch it to schedule alarm properly
            if (task.alarmEnabled && task.dueDate != null && task.dueTime != null) {
                // Use a copy with the inserted id if auto-generated
                val taskWithId = if (task.id == 0) task.copy(id = inserted.toInt()) else task
                AlarmScheduler.schedule(context, taskWithId)
            }
        }
    }

    /** Update an existing task and re-schedule its alarm. */
    fun updateTask(context: Context, task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
            AlarmScheduler.cancel(context, task) // cancel old alarm
            if (task.alarmEnabled && task.dueDate != null && task.dueTime != null) {
                AlarmScheduler.schedule(context, task)
            }
        }
    }

    fun toggleDone(context: Context, task: Task) {
        viewModelScope.launch {
            repository.toggleDone(task)
            // When a task is completed, cancel its alarm
            if (!task.isDone) AlarmScheduler.cancel(context, task)
        }
    }

    fun deleteTask(context: Context, task: Task) {
        viewModelScope.launch {
            AlarmScheduler.cancel(context, task)
            repository.deleteTask(task)
        }
    }

    // ── AI: Create from text ───────────────────────────────────────────────────
    fun createTaskFromText(context: Context, text: String) {
        if (text.isBlank()) return
        val token = TokenManager.getToken(context) ?: run {
            _aiState.value = _aiState.value.copy(aiError = "Please log in first")
            return
        }
        viewModelScope.launch {
            _aiState.value = _aiState.value.copy(isAiLoading = true, aiError = null, aiSuccess = null)
            try {
                val resp = RetrofitClient.api.createTaskFromText("Bearer $token", text)
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val taskTitle = (body?.task?.get("title") as? String) ?: text
                    val subtasksJson = com.google.gson.Gson().toJson(body?.getSubtasks())
                    val note = body?.getDescription().orEmpty().ifBlank { "Created by AI" }
                    repository.addTask(Task(title = taskTitle, note = note, subtasksJson = subtasksJson))
                    _aiState.value = _aiState.value.copy(isAiLoading = false,
                        aiSuccess = "✅ Task created: \"$taskTitle\"")
                } else if (resp.code() == 401) {
                    TokenManager.clearToken(context)
                    _aiState.value = _aiState.value.copy(isAiLoading = false,
                        aiError = "Session expired. Please log in again.")
                } else {
                    _aiState.value = _aiState.value.copy(isAiLoading = false,
                        aiError = "AI failed (${resp.code()})")
                }
            } catch (e: Exception) {
                // Fallback: save text directly as task when backend is offline
                repository.addTask(Task(title = text, note = "Manual — AI unavailable"))
                _aiState.value = _aiState.value.copy(isAiLoading = false,
                    aiSuccess = "Task saved locally (backend offline)")
            }
        }
    }

    // ── AI: Create from image (OCR) ───────────────────────────────────────────
    fun createTaskFromImage(context: Context, imageUri: Uri) {
        val token = TokenManager.getToken(context) ?: run {
            _aiState.value = _aiState.value.copy(aiError = "Please log in first")
            return
        }
        viewModelScope.launch {
            _aiState.value = _aiState.value.copy(isAiLoading = true, aiError = null, aiSuccess = null)
            try {
                val file = uriToFile(context, imageUri)
                val imagePart = MultipartBody.Part.createFormData(
                    "image", file.name,
                    file.asRequestBody("image/*".toMediaType())
                )
                val langBody = "en".toRequestBody("text/plain".toMediaType())
                val extractBody = "false".toRequestBody("text/plain".toMediaType())
                val resp = RetrofitClient.api.createTaskFromScreenshot(
                    "Bearer $token", imagePart, langBody, extractBody
                )
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val taskTitle = (body?.task?.get("title") as? String) ?: "Task from image"
                    val subtasksJson = com.google.gson.Gson().toJson(body?.getSubtasks())
                    val note = body?.getDescription().orEmpty().ifBlank { "Created from image OCR" }
                    repository.addTask(Task(title = taskTitle, note = note, subtasksJson = subtasksJson))
                    _aiState.value = _aiState.value.copy(isAiLoading = false,
                        aiSuccess = "📷 Task created: \"$taskTitle\"")
                } else if (resp.code() == 401) {
                    TokenManager.clearToken(context)
                    _aiState.value = _aiState.value.copy(isAiLoading = false,
                        aiError = "Session expired. Please log in again.")
                } else {
                    _aiState.value = _aiState.value.copy(isAiLoading = false,
                        aiError = "Image OCR failed (${resp.code()})")
                }
            } catch (e: Exception) {
                _aiState.value = _aiState.value.copy(isAiLoading = false,
                    aiError = "Error: ${e.localizedMessage}")
            }
        }
    }

    // ── AI: Create from voice recording ──────────────────────────────────────
    fun createTaskFromVoice(context: Context, audioFile: File) {
        val token = TokenManager.getToken(context) ?: run {
            _aiState.value = _aiState.value.copy(aiError = "Please log in first")
            return
        }
        viewModelScope.launch {
            _aiState.value = _aiState.value.copy(isAiLoading = true, aiError = null, aiSuccess = null)
            try {
                val audioPart = MultipartBody.Part.createFormData(
                    "audio", audioFile.name,
                    audioFile.asRequestBody("audio/m4a".toMediaType())
                )
                val langBody = "en".toRequestBody("text/plain".toMediaType())
                val extractBody = "false".toRequestBody("text/plain".toMediaType())
                val resp = RetrofitClient.api.createTaskFromVoice(
                    "Bearer $token", audioPart, langBody, extractBody
                )
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val taskTitle = (body?.task?.get("title") as? String) ?: "Voice task"
                    val transcript = body?.transcript
                    repository.addTask(Task(
                        title = taskTitle,
                        note = if (transcript != null) "Transcript: $transcript" else "Created from voice"
                    ))
                    _aiState.value = _aiState.value.copy(isAiLoading = false,
                        aiSuccess = "🎤 Task created: \"$taskTitle\"")
                } else if (resp.code() == 401) {
                    TokenManager.clearToken(context)
                    _aiState.value = _aiState.value.copy(isAiLoading = false,
                        aiError = "Session expired. Please log in again.")
                } else {
                    _aiState.value = _aiState.value.copy(isAiLoading = false,
                        aiError = "Voice STT failed (${resp.code()})")
                }
            } catch (e: Exception) {
                _aiState.value = _aiState.value.copy(isAiLoading = false,
                    aiError = "Error: ${e.localizedMessage}")
            }
        }
    }

    fun clearAiMessages() {
        _aiState.value = _aiState.value.copy(aiError = null, aiSuccess = null)
    }

    // ── Sync backend tasks → local Room DB ────────────────────────────────────
    // Called on screen open so tasks created via Chat (stored in MongoDB) appear here too.
    fun syncFromBackend(context: Context) {
        val token = TokenManager.getToken(context) ?: return
        viewModelScope.launch {
            try {
                val resp = RetrofitClient.api.getBackendTasks("Bearer $token")
                if (resp.isSuccessful) {
                    val backendTasks = resp.body() ?: emptyList()
                    val localTitles = repository.getAllTasksTitles()
                    backendTasks.forEach { bt ->
                        // Only insert if not already in Room (match by title to avoid duplicates)
                        if (bt.title.isNotBlank() && !localTitles.contains(bt.title)) {
                            val priority = when (bt.priority.lowercase()) {
                                "high" -> Priority.HIGH
                                "low"  -> Priority.LOW
                                else   -> Priority.NORMAL
                            }
                            repository.addTask(
                                Task(
                                    title = bt.title,
                                    note = bt.description ?: if (bt.aiGenerated) "Created by AI" else "",
                                    priority = priority,
                                    isDone = bt.status == "completed"
                                )
                            )
                        }
                    }
                } else if (resp.code() == 401) {
                    TokenManager.clearToken(context)
                }
            } catch (_: Exception) {
                /* Silent — offline is fine, local DB still shows */
            }
        }
    }


    private fun uriToFile(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)!!
        val file = File.createTempFile("img_", ".jpg", context.cacheDir)
        FileOutputStream(file).use { inputStream.copyTo(it) }
        return file
    }
}
