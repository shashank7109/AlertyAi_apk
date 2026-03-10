package com.alertyai.app.ui.tasks

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alertyai.app.data.model.CheckItem
import com.alertyai.app.data.model.Priority
import com.alertyai.app.data.model.Task
import com.alertyai.app.network.TokenManager
import com.alertyai.app.ui.components.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    isLoggedIn: Boolean,
    isAiLoading: Boolean,
    onDismiss: () -> Unit,
    onAddTask: (Task) -> Unit,
    onUpdateTask: (Task) -> Unit = {},
    onAddFromText: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onVoiceFile: (File) -> Unit,
    existingTask: Task? = null,
    initialMode: String = "manual",
    autoStartVoice: Boolean = false
) {
    val isEditMode = existingTask != null
    val context = LocalContext.current
    var mode by remember { mutableStateOf(if (autoStartVoice) "voice" else initialMode) }

    val gson = remember { Gson() }
    val initialSubtasks = remember {
        existingTask?.subtasksJson?.let {
            try { gson.fromJson<List<String>>(it, object : com.google.gson.reflect.TypeToken<List<String>>() {}.type) ?: emptyList() }
            catch (_: Exception) { emptyList() }
        } ?: emptyList()
    }
    val initialChecklist = remember {
        existingTask?.checklistJson?.let {
            try { gson.fromJson<List<CheckItem>>(it, object : com.google.gson.reflect.TypeToken<List<CheckItem>>() {}.type) ?: emptyList() }
            catch (_: Exception) { emptyList() }
        } ?: emptyList()
    }

    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var note by remember { mutableStateOf(existingTask?.note ?: "") }
    var priority by remember { mutableStateOf(existingTask?.priority ?: Priority.NORMAL) }
    var repeatInterval by remember { mutableStateOf(existingTask?.repeatInterval ?: com.alertyai.app.data.model.RepeatInterval.NONE) }
    var dueDate by remember { mutableStateOf<Long?>(existingTask?.dueDate) }
    var dueTime by remember { mutableStateOf<Long?>(existingTask?.dueTime) }
    var alarmEnabled by remember { mutableStateOf(existingTask?.alarmEnabled ?: false) }
    var remindMinsBefore by remember { mutableStateOf(existingTask?.remindMinsBefore ?: 10) }
    var location by remember { mutableStateOf(existingTask?.location ?: "") }
    var showLocationField by remember { mutableStateOf(existingTask?.location?.isNotBlank() ?: false) }
    var subtasks by remember { mutableStateOf(initialSubtasks) }
    var newSubtask by remember { mutableStateOf("") }
    var checklist by remember { mutableStateOf(initialChecklist) }
    var newCheckItem by remember { mutableStateOf("") }

    var aiText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var recognitionError by remember { mutableStateOf<String?>(null) }
    val speechRecognizer = remember { android.speech.SpeechRecognizer.createSpeechRecognizer(context) }
    val recognizerIntent = remember {
        android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
    }

    DisposableEffect(Unit) {
        val listener = object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isRecording = false }
            override fun onError(error: Int) {
                isRecording = false
                recognitionError = when (error) {
                    android.speech.SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected."
                    android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out."
                    else -> "Recording error ($error)"
                }
            }
            override fun onResults(results: android.os.Bundle?) {
                isRecording = false
                val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    onAddFromText(text) // send as text to create task
                    onDismiss()
                } else {
                    recognitionError = "Didn't catch that. Please try again."
                }
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose { speechRecognizer.destroy() }
    }

    val dateFormat = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    val cal = Calendar.getInstance()

    fun showDatePicker() {
        val dialog = android.app.DatePickerDialog(
            context,
            { _, y, m, d ->
                val c = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                dueDate = c.timeInMillis
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.minDate = System.currentTimeMillis() - 1000
        dialog.show()
    }

    fun showTimePicker() {
        android.app.TimePickerDialog(
            context,
            { _, h, min ->
                val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, min); set(Calendar.SECOND, 0) }
                dueTime = c.timeInMillis
                if (dueDate == null) dueDate = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
            },
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false
        ).show()
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onImageSelected(it); onDismiss() }
    }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            recognitionError = null
            isRecording = true
            speechRecognizer.startListening(recognizerIntent)
        } else {
            recognitionError = "Microphone permission required"
        }
    }

    LaunchedEffect(autoStartVoice) {
        if (autoStartVoice) {
            // Give a tiny delay for dialog to compose before popping permission
            delay(300)
            audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) { delay(1000); recordingSeconds++ }
        }
    }

    fun buildAndSaveTask() {
        if (title.isBlank()) return
        val task = Task(
            id = existingTask?.id ?: 0,
            title = title.trim(),
            note = note.trim(),
            priority = priority,
            dueDate = dueDate,
            dueTime = dueTime,
            repeatInterval = repeatInterval,
            alarmEnabled = alarmEnabled && dueDate != null,
            remindMinsBefore = remindMinsBefore,
            location = location.trim(),
            subtasksJson = gson.toJson(subtasks),
            checklistJson = gson.toJson(checklist),
            isDone = existingTask?.isDone ?: false,
            createdAt = existingTask?.createdAt ?: System.currentTimeMillis(),
            backendId = existingTask?.backendId ?: ""
        )
        if (isEditMode) onUpdateTask(task) else onAddTask(task)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (isEditMode) "Edit Task" else "New Task",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (isEditMode) "Update Task" else "Create Task",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null)
                }
            }

            // Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple(Icons.Default.Edit, "manual", "TEXT"),
                    Triple(Icons.Default.AutoAwesome, "ai_text", "AI"),
                    Triple(Icons.Default.PhotoCamera, "image", "SCAN"),
                    Triple(Icons.Default.Mic, "voice", "VOICE")
                ).forEach { (icon, m, label) ->
                    if (!isLoggedIn && m != "manual") return@forEach
                    val isActive = mode == m
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { mode = m }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                icon, null, 
                                modifier = Modifier.size(16.dp),
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Medium,
                                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (!isLoggedIn && mode == "manual") {
                Text(
                    "🔐 Log in for AI features", 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            when (mode) {
                "manual" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ClayCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                TextField(
                                    value = title, onValueChange = { title = it },
                                    placeholder = { Text("Task title") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent
                                    ),
                                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                TextField(
                                    value = note, onValueChange = { note = it },
                                    placeholder = { Text("Notes (optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent
                                    ),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        // Priority 
                        Column {
                            Text("PRIORITY LEVEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Priority.values().forEach { p ->
                                    val color = when (p) {
                                        Priority.HIGH -> Color(0xFFEF4444)
                                        Priority.NORMAL -> Color(0xFF3B82F6)
                                        Priority.LOW -> Color(0xFF10B981)
                                    }
                                    val isActive = priority == p
                                    ClayCard(
                                        onClick = { priority = p },
                                        containerColor = if (isActive) color else MaterialTheme.colorScheme.surface,
                                        elevation = if (isActive) 0.dp else 4.dp,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            p.name, 
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isActive) Color.White else color,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Recurrence
                        Column {
                            Text("RECURRENCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                com.alertyai.app.data.model.RepeatInterval.values().forEach { r ->
                                    val isActive = repeatInterval == r
                                    ClayCard(
                                        onClick = { repeatInterval = r },
                                        containerColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        elevation = if (isActive) 0.dp else 4.dp,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            r.name, 
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Date & Time 
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ClayCard(modifier = Modifier.weight(1f), onClick = { showDatePicker() }) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        dueDate?.let { dateFormat.format(Date(it)).uppercase() } ?: "SELECT DATE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            ClayCard(modifier = Modifier.weight(1f), onClick = { showTimePicker() }) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        dueTime?.let { timeFormat.format(Date(it)).uppercase() } ?: "SELECT TIME",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Checklist
                        Column {
                            Text("SUBTASKS / CHECKLIST", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            ClayCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    checklist.forEachIndexed { index, item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(
                                                checked = item.done,
                                                onCheckedChange = { checked ->
                                                    val newList = checklist.toMutableList()
                                                    newList[index] = item.copy(done = checked)
                                                    checklist = newList
                                                }
                                            )
                                            TextField(
                                                value = item.text,
                                                onValueChange = { text ->
                                                    val newList = checklist.toMutableList()
                                                    newList[index] = item.copy(text = text)
                                                    checklist = newList
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = TextFieldDefaults.colors(
                                                    unfocusedContainerColor = Color.Transparent,
                                                    focusedContainerColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent,
                                                    focusedIndicatorColor = Color.Transparent
                                                ),
                                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                    textDecoration = if (item.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                                )
                                            )
                                            IconButton(
                                                onClick = {
                                                    val newList = checklist.toMutableList()
                                                    newList.removeAt(index)
                                                    checklist = newList
                                                }
                                            ) {
                                                Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.primary)
                                        TextField(
                                            value = newCheckItem,
                                            onValueChange = { newCheckItem = it },
                                            placeholder = { Text("Add new subtask...") },
                                            modifier = Modifier.weight(1f),
                                            colors = TextFieldDefaults.colors(
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent
                                            ),
                                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                                onDone = {
                                                    if (newCheckItem.isNotBlank()) {
                                                        checklist = checklist + CheckItem(text = newCheckItem.trim())
                                                        newCheckItem = ""
                                                    }
                                                }
                                            ),
                                            singleLine = true
                                        )
                                        if (newCheckItem.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    checklist = checklist + CheckItem(text = newCheckItem.trim())
                                                    newCheckItem = ""
                                                }
                                            ) {
                                                Icon(Icons.Default.Check, "Add Item", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Alarm
                        AnimatedVisibility(visible = dueDate != null || dueTime != null) {
                            ClayCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(
                                            Icons.Default.NotificationsActive, null, 
                                            tint = if (alarmEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text("ALERTS ENABLED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                                    }
                                    Switch(checked = alarmEnabled, onCheckedChange = { alarmEnabled = it })
                                }
                            }
                        }

                        // Save Button
                        ClayButton(
                            onClick = { buildAndSaveTask() },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(if (isEditMode) Icons.Default.Sync else Icons.Default.Check, null, tint = Color.White)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (isEditMode) "Update Task" else "Create Task",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                "ai_text" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ClayCard(modifier = Modifier.fillMaxWidth()) {
                            TextField(
                                value = aiText, onValueChange = { aiText = it },
                                placeholder = { Text("Describe what you need to do...") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                        }
                        ClayButton(
                            onClick = { if (aiText.isNotBlank()) onAddFromText(aiText) },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.primary,
                            enabled = aiText.isNotBlank() && !isAiLoading
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 3.dp, color = Color.White)
                            } else {
                                Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
                                Spacer(Modifier.width(10.dp))
                                Text("SYNTHESIZE TASK", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                "image" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SCAN PARAMETERS FROM PHYSICAL DATA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ClayButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.PhotoLibrary, null)
                                Spacer(Modifier.width(8.dp))
                                Text("GALLERY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                            }
                            ClayButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.CameraAlt, null)
                                Spacer(Modifier.width(8.dp))
                                Text("CAMERA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                "voice" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Record voice note", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                shape = CircleShape,
                                color = if (isRecording) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) 
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                modifier = Modifier.size(160.dp)
                            ) {}
                            
                            FloatingActionButton(
                                onClick = {
                                    if (!isRecording) {
                                        audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        speechRecognizer.stopListening()
                                        isRecording = false
                                    }
                                },
                                containerColor = if (isRecording) MaterialTheme.colorScheme.error
                                                 else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(100.dp),
                                shape = CircleShape,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 12.dp)
                            ) {
                                Icon(
                                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic, 
                                    null, tint = Color.White, modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        if (isRecording) {
                            Text("LISTENING: ${recordingSeconds}S", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                            Text("Will auto-stop when you finish speaking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        } else {
                            Text("Ready to record", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        recognitionError?.let { err ->
                            Text(err, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
