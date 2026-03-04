package com.alertyai.app.ui.tasks

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.media.MediaRecorder
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
    initialMode: String = "manual"
) {
    val isEditMode = existingTask != null
    val context = LocalContext.current
    var mode by remember { mutableStateOf(initialMode) }

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
    var recorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: File? by remember { mutableStateOf(null) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var tooShortWarning by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    val cal = Calendar.getInstance()

    fun showDatePicker() {
        android.app.DatePickerDialog(
            context,
            { _, y, m, d ->
                val c = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                dueDate = c.timeInMillis
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
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
            audioFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            @Suppress("DEPRECATION")
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
                isRecording = true
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            tooShortWarning = false
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
            alarmEnabled = alarmEnabled && dueDate != null,
            remindMinsBefore = remindMinsBefore,
            location = location.trim(),
            subtasksJson = gson.toJson(subtasks),
            checklistJson = gson.toJson(checklist),
            isDone = existingTask?.isDone ?: false,
            createdAt = existingTask?.createdAt ?: System.currentTimeMillis()
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
                        if (isEditMode) "TASK RECALIBRATION" else "NEW OBJECTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (isEditMode) "UPDATE PARAMETERS" else "INITIALIZE MISSION",
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
                    "🔐 LOG IN FOR NEURAL FEATURES", 
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
                                    placeholder = { Text("MISSION TITLE") },
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
                                    placeholder = { Text("ADDITIONAL INTEL") },
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
                                if (isEditMode) "UPDATE MISSION" else "COMMENCE MISSION",
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
                                placeholder = { Text("DESCRIBE OBJECTIVE IN NEURAL TEXT...") },
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
                        Text("RECORD VOCAL COMMAND", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
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
                                        tooShortWarning = false
                                        audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        if (recordingSeconds < 2) {
                                            recorder?.stop(); recorder?.release(); recorder = null
                                            isRecording = false; tooShortWarning = true
                                        } else {
                                            recorder?.stop(); recorder?.release(); recorder = null
                                            isRecording = false
                                            audioFile?.let { onVoiceFile(it); onDismiss() }
                                        }
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
                            Text("TRANSMITTING: ${recordingSeconds}S", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("READY FOR SIGNAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (tooShortWarning) {
                            Text("SIGNAL TOO WEAK (MIN 2S)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
