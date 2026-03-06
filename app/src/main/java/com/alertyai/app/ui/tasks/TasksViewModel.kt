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

    private val userWsManager = com.alertyai.app.network.UserWebSocketManager()

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

            // Sync with backend if it exists there
            if (task.backendId.isNotBlank()) {
                val token = TokenManager.getToken(context) ?: return@launch
                try {
                    var dueDateStr: String? = null
                    if (task.dueDate != null) {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }
                        // Combine dueDate and dueTime for the backend if both exist, otherwise just dueDate
                        val timeToUse = if (task.dueTime != null) {
                            val dCal = java.util.Calendar.getInstance().apply { timeInMillis = task.dueDate }
                            val tCal = java.util.Calendar.getInstance().apply { timeInMillis = task.dueTime }
                            dCal.set(java.util.Calendar.HOUR_OF_DAY, tCal.get(java.util.Calendar.HOUR_OF_DAY))
                            dCal.set(java.util.Calendar.MINUTE, tCal.get(java.util.Calendar.MINUTE))
                            dCal.timeInMillis
                        } else {
                            task.dueDate
                        }
                        dueDateStr = sdf.format(java.util.Date(timeToUse))
                    }

                    val updateRequest = com.alertyai.app.network.BackendTaskUpdate(
                        title = task.title,
                        description = task.note,
                        priority = task.priority.name.lowercase(),
                        status = if (task.isDone) "completed" else "pending",
                        dueDate = dueDateStr
                    )
                    RetrofitClient.api.updateBackendTask("Bearer $token", task.backendId, updateRequest)
                } catch (e: Exception) {
                    // Fail silently, local copy is saved
                }
            }
        }
    }

    fun toggleDone(context: Context, task: Task) {
        viewModelScope.launch {
            repository.toggleDone(task)
            
            // Handle backend sync if applicable
            if (!task.isDone && task.backendId.isNotBlank()) {
                val token = TokenManager.getToken(context) ?: return@launch
                try {
                    RetrofitClient.api.completeBackendTask("Bearer $token", task.backendId)
                } catch (e: Exception) {
                    // Fail silently, DB completes it locally anyway. Re-sync pulls state from backend though, 
                    // ideally we want a background offline-sync job, but for now this fixes the immediate parity issue.
                }
            }
            
            // When a task is completed, cancel its alarm
            if (!task.isDone) AlarmScheduler.cancel(context, task)
        }
    }

    fun deleteTask(context: Context, task: Task) {
        viewModelScope.launch {
            AlarmScheduler.cancel(context, task)
            repository.deleteTask(task)
            // If this task was created on the backend, delete it there too —
            // otherwise syncFromBackend will re-insert it on next screen open.
            if (task.backendId.isNotBlank()) {
                val token = TokenManager.getToken(context) ?: return@launch
                try {
                    RetrofitClient.api.deleteBackendTask("Bearer $token", task.backendId)
                } catch (_: Exception) {
                    // Silent: local delete already happened; backend will be cleaned up on next sync
                }
            }
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
                    val taskMap = body?.task ?: emptyMap()
                    val taskTitle = (taskMap["title"] as? String) ?: text
                    val checklistJson = com.google.gson.Gson().toJson(
                        body?.getSubtasks()?.map { com.alertyai.app.data.model.CheckItem(text = it) } ?: emptyList<com.alertyai.app.data.model.CheckItem>()
                    )
                    val note = body?.getDescription().orEmpty().ifBlank { "Created by AI" }
                    val dueDateStr = taskMap["due_date"] as? String
                    val dueTimeStr = taskMap["due_time"] as? String
                    val priorityStr = taskMap["priority"] as? String
                    val backendId = (taskMap["_id"] as? String).orEmpty()

                    addFullTask(context, Task(
                        title = taskTitle,
                        note = note,
                        checklistJson = checklistJson,
                        dueDate = parseDateToMillis(dueDateStr),
                        dueTime = parseTimeToMillis(dueTimeStr),
                        priority = parsePriority(priorityStr),
                        alarmEnabled = true,
                        backendId = backendId
                    ))
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
                    val taskMap = body?.task ?: emptyMap()
                    val taskTitle = (taskMap["title"] as? String) ?: "Task from image"
                    val checklistJson = com.google.gson.Gson().toJson(
                        body?.getSubtasks()?.map { com.alertyai.app.data.model.CheckItem(text = it) } ?: emptyList<com.alertyai.app.data.model.CheckItem>()
                    )
                    val note = body?.getDescription().orEmpty().ifBlank { "Created from image OCR" }
                    val dueDateStr = taskMap["due_date"] as? String
                    val dueTimeStr = taskMap["due_time"] as? String
                    val priorityStr = taskMap["priority"] as? String
                    val backendId = (taskMap["_id"] as? String).orEmpty()

                    addFullTask(context, Task(
                        title = taskTitle,
                        note = note,
                        checklistJson = checklistJson,
                        dueDate = parseDateToMillis(dueDateStr),
                        dueTime = parseTimeToMillis(dueTimeStr),
                        priority = parsePriority(priorityStr),
                        alarmEnabled = true,
                        backendId = backendId
                    ))
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
                    val taskMap = body?.task ?: emptyMap()
                    val taskTitle = (taskMap["title"] as? String) ?: "Voice task"
                    val transcript = body?.transcript
                    val checklistJson = com.google.gson.Gson().toJson(
                        body?.getSubtasks()?.map { com.alertyai.app.data.model.CheckItem(text = it) } ?: emptyList<com.alertyai.app.data.model.CheckItem>()
                    )
                    val dueDateStr = taskMap["due_date"] as? String
                    val dueTimeStr = taskMap["due_time"] as? String
                    val priorityStr = taskMap["priority"] as? String
                    val backendId = (taskMap["_id"] as? String).orEmpty()

                    addFullTask(context, Task(
                        title = taskTitle,
                        note = if (transcript != null) "Transcript: $transcript" else "Created from voice",
                        checklistJson = checklistJson,
                        dueDate = parseDateToMillis(dueDateStr),
                        dueTime = parseTimeToMillis(dueTimeStr),
                        priority = parsePriority(priorityStr),
                        alarmEnabled = true,
                        backendId = backendId
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
        
        // Setup Real-Time Sync WebSocket 
        val userId = TokenManager.getUserId(context)
        if (userId != null && !userWsManager.isConnected) {
            userWsManager.connect(
                userId = userId,
                token = token,
                onMessage = { msg ->
                    if (msg.type == "task_created" && msg.task != null) {
                        val bt = msg.task
                        viewModelScope.launch {
                            val allLocal = repository.getAllTasksList() // Needs to be added to repo
                            val existing = allLocal.find { it.backendId == bt.id || (bt.title.isNotBlank() && it.title == bt.title) }
                            
                            if (existing == null) {
                                val priority = when (bt.priority.lowercase()) {
                                    "high" -> Priority.HIGH
                                    "low"  -> Priority.LOW
                                    else   -> Priority.NORMAL
                                }
                                val checklistJson = com.google.gson.Gson().toJson(
                                    bt.subtasks?.map { com.alertyai.app.data.model.CheckItem(text = it) }
                                        ?: emptyList<com.alertyai.app.data.model.CheckItem>()
                                )
                                val dueDateMillis = parseDateToMillis(bt.dueDate)
                                val dueTimeMillis = parseTimeToMillis(bt.dueTime)
                                    ?: run {
                                        val isoTime = bt.dueDate?.takeIf { it.contains("T") }
                                            ?.substringAfter("T")?.take(5)
                                        parseTimeToMillis(isoTime?.takeIf { it != "00:00" })
                                    }

                                val noteText = buildString {
                                    if (!bt.description.isNullOrBlank()) append(bt.description)
                                    if (bt.isTeamTask && !bt.teamName.isNullOrBlank()) {
                                        if (isNotEmpty()) append("\n")
                                        append("Team: ${bt.teamName}")
                                    }
                                    if (isEmpty()) append(if (bt.aiGenerated) "Created by AI" else "")
                                }

                                val hasTime = dueDateMillis != null && dueTimeMillis != null
                                val isCompleted = bt.status == "completed"
                                val newTask = Task(
                                    title     = bt.title,
                                    note      = noteText,
                                    priority  = priority,
                                    isDone    = isCompleted,
                                    checklistJson = checklistJson,
                                    dueDate   = dueDateMillis,
                                    dueTime   = dueTimeMillis,
                                    alarmEnabled = hasTime && !isCompleted,
                                    backendId = bt.id
                                )
                                if (!isCompleted && hasTime) {
                                    addFullTask(context, newTask)
                                } else {
                                    repository.addTask(newTask)
                                }
                            }
                        }
                    }
                }
            )
        }

        viewModelScope.launch {
            try {
                val resp = RetrofitClient.api.getBackendTasks("Bearer $token")
                if (resp.isSuccessful) {
                    val backendTasks = resp.body() ?: emptyList()
                    val allLocal = repository.getAllTasksList()
                    
                    backendTasks.forEach { bt ->
                        val existing = allLocal.find { it.backendId == bt.id || (bt.title.isNotBlank() && it.title == bt.title) }
                        
                        if (existing == null) {
                            val priority = when (bt.priority.lowercase()) {
                                "high" -> Priority.HIGH
                                "low"  -> Priority.LOW
                                else   -> Priority.NORMAL
                            }
                            val checklistJson = com.google.gson.Gson().toJson(
                                bt.subtasks?.map { com.alertyai.app.data.model.CheckItem(text = it) }
                                    ?: emptyList<com.alertyai.app.data.model.CheckItem>()
                            )

                            val dueDateMillis = parseDateToMillis(bt.dueDate)
                            val dueTimeMillis = parseTimeToMillis(bt.dueTime)
                                ?: run {
                                    val isoTime = bt.dueDate?.takeIf { it.contains("T") }
                                        ?.substringAfter("T")?.take(5)
                                    parseTimeToMillis(isoTime?.takeIf { it != "00:00" })
                                }

                            val noteText = buildString {
                                if (!bt.description.isNullOrBlank()) append(bt.description)
                                if (bt.isTeamTask && !bt.teamName.isNullOrBlank()) {
                                    if (isNotEmpty()) append("\n")
                                    append("Team: ${bt.teamName}")
                                }
                                if (isEmpty()) append(if (bt.aiGenerated) "Created by AI" else "")
                            }

                            val hasTime = dueDateMillis != null && dueTimeMillis != null
                            val isCompleted = bt.status == "completed"
                            val newTask = Task(
                                title     = bt.title,
                                note      = noteText,
                                priority  = priority,
                                isDone    = isCompleted,
                                checklistJson = checklistJson,
                                dueDate   = dueDateMillis,
                                dueTime   = dueTimeMillis,
                                alarmEnabled = hasTime && !isCompleted,
                                backendId = bt.id
                            )
                            if (!isCompleted && hasTime) {
                                addFullTask(context, newTask)
                            } else {
                                repository.addTask(newTask)
                            }
                        } else if (existing.backendId.isBlank() && bt.id.isNotBlank()) {
                             // Link the local task to the backend id
                             repository.updateTask(existing.copy(backendId = bt.id))
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

    private fun parseDateToMillis(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            if (dateStr.contains("T")) {
                val clean = dateStr.substringBefore(".")
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(clean)?.time
            } else {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dateStr)?.time
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTimeToMillis(timeStr: String?): Long? {
        if (timeStr.isNullOrBlank()) return null
        return try {
            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).parse(timeStr)?.time
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

    override fun onCleared() {
        super.onCleared()
        userWsManager.disconnect()
    }
}
