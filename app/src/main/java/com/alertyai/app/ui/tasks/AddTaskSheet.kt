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
import com.google.gson.Gson
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
    onAddTask: (Task) -> Unit,                    // full Task object
    onAddFromText: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onVoiceFile: (File) -> Unit
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf("manual") }

    // ── Manual task fields ────────────────────────────────────────────────
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var dueTime by remember { mutableStateOf<Long?>(null) }
    var alarmEnabled by remember { mutableStateOf(false) }
    var remindMinsBefore by remember { mutableStateOf(10) }
    var location by remember { mutableStateOf("") }
    var showLocationField by remember { mutableStateOf(false) }
    var subtasks by remember { mutableStateOf(listOf<String>()) }
    var newSubtask by remember { mutableStateOf("") }
    var showSubtasks by remember { mutableStateOf(false) }
    var checklist by remember { mutableStateOf(listOf<CheckItem>()) }
    var newCheckItem by remember { mutableStateOf("") }
    var showChecklist by remember { mutableStateOf(false) }

    // ── AI / Voice fields ─────────────────────────────────────────────────
    var aiText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var recorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: File? by remember { mutableStateOf(null) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var tooShortWarning by remember { mutableStateOf(false) }

    // Date/time formatters
    val dateFormat = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // ── Pickers ───────────────────────────────────────────────────────────
    val cal = Calendar.getInstance()

    fun showDatePicker() {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val c = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                dueDate = c.timeInMillis
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showTimePicker() {
        TimePickerDialog(
            context,
            { _, h, min ->
                val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, min); set(Calendar.SECOND, 0) }
                dueTime = c.timeInMillis
                if (dueDate == null) dueDate = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
            },
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false
        ).show()
    }

    // ── Image & permission launchers ──────────────────────────────────────
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

    // Live recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            tooShortWarning = false
            while (isRecording) { delay(1000); recordingSeconds++ }
        }
    }

    // ── Build Task and save ───────────────────────────────────────────────
    fun buildAndSaveTask() {
        if (title.isBlank()) return
        val gson = Gson()
        onAddTask(
            Task(
                title = title.trim(),
                note = note.trim(),
                priority = priority,
                dueDate = dueDate,
                dueTime = dueTime,
                alarmEnabled = alarmEnabled && dueDate != null,
                remindMinsBefore = remindMinsBefore,
                location = location.trim(),
                subtasksJson = gson.toJson(subtasks),
                checklistJson = gson.toJson(checklist)
            )
        )
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Add Task", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            // ── Mode tabs ─────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("✍️" to "manual", "🤖" to "ai_text", "📷" to "image", "🎤" to "voice").forEach { (icon, m) ->
                    if (!isLoggedIn && m != "manual") return@forEach
                    FilterChip(selected = mode == m, onClick = { mode = m },
                        label = { Text(icon, fontSize = 16.sp) })
                }
            }
            if (!isLoggedIn) {
                Text("🔐 Log in to use AI, image OCR, and voice.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            when (mode) {
                // ── MANUAL ─────────────────────────────────────────────────
                "manual" -> {
                    // Title
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text("Task title *") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    // Note
                    OutlinedTextField(
                        value = note, onValueChange = { note = it },
                        label = { Text("Note") }, modifier = Modifier.fillMaxWidth(), maxLines = 3
                    )

                    // ── Priority ──────────────────────────────────────────
                    Text("Priority", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Priority.values().forEach { p ->
                            val (icon, color) = when (p) {
                                Priority.HIGH   -> "🔴" to Color(0xFFEF4444)
                                Priority.NORMAL -> "🟡" to Color(0xFFF97316)
                                Priority.LOW    -> "🟢" to Color(0xFF22C55E)
                            }
                            FilterChip(
                                selected = priority == p, onClick = { priority = p },
                                label = { Text("$icon ${p.name.lowercase().replaceFirstChar { it.uppercase() }}") }
                            )
                        }
                    }

                    // ── Date & Time ───────────────────────────────────────
                    Text("Date & Time", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        // Date button
                        OutlinedButton(
                            onClick = { showDatePicker() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(dueDate?.let { dateFormat.format(Date(it)) } ?: "Set Date", fontSize = 12.sp)
                        }
                        // Time button
                        OutlinedButton(
                            onClick = { showTimePicker() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(dueTime?.let { timeFormat.format(Date(it)) } ?: "Set Time", fontSize = 12.sp)
                        }
                    }

                    // ── Alarm / Reminder ──────────────────────────────────
                    AnimatedVisibility(visible = dueDate != null || dueTime != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Alarm, null, modifier = Modifier.size(20.dp),
                                        tint = if (alarmEnabled) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Set Alarm", style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(checked = alarmEnabled, onCheckedChange = { alarmEnabled = it })
                            }

                            AnimatedVisibility(visible = alarmEnabled) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Remind me", style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(0 to "At time", 10 to "10 min", 30 to "30 min", 60 to "1 hr").forEach { (mins, label) ->
                                            FilterChip(
                                                selected = remindMinsBefore == mins,
                                                onClick = { remindMinsBefore = mins },
                                                label = { Text(label, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Location (optional toggle) ─────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showLocationField = !showLocationField }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(20.dp),
                            tint = if (showLocationField) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (location.isBlank()) "Add location (optional)" else location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (showLocationField) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AnimatedVisibility(visible = showLocationField) {
                        OutlinedTextField(
                            value = location, onValueChange = { location = it },
                            label = { Text("Location / Place") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true
                        )
                    }

                    // ── Subtasks (optional toggle) ─────────────────────────
                    HorizontalDivider(thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showSubtasks = !showSubtasks }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(20.dp),
                                tint = if (showSubtasks) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Subtasks ${if (subtasks.isNotEmpty()) "(${subtasks.size})" else "(optional)"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (showSubtasks) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (showSubtasks) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    AnimatedVisibility(visible = showSubtasks) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            subtasks.forEachIndexed { i, s ->
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Circle, null, modifier = Modifier.size(8.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                    Text(s, modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall)
                                    IconButton(onClick = { subtasks = subtasks.filterIndexed { idx, _ -> idx != i } },
                                        modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = newSubtask, onValueChange = { newSubtask = it },
                                    placeholder = { Text("Add subtask…", fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f), singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                )
                                IconButton(
                                    onClick = {
                                        if (newSubtask.isNotBlank()) {
                                            subtasks = subtasks + newSubtask.trim()
                                            newSubtask = ""
                                        }
                                    }
                                ) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) }
                            }
                        }
                    }

                    // ── Checklist (optional toggle) ────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showChecklist = !showChecklist }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Checklist, null, modifier = Modifier.size(20.dp),
                                tint = if (showChecklist) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Checklist ${if (checklist.isNotEmpty()) "(${checklist.size})" else "(optional)"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (showChecklist) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (showChecklist) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    AnimatedVisibility(visible = showChecklist) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            checklist.forEachIndexed { i, item ->
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = item.done,
                                        onCheckedChange = {
                                            checklist = checklist.toMutableList().also { l -> l[i] = item.copy(done = it) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(item.text, modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall)
                                    IconButton(onClick = { checklist = checklist.filterIndexed { idx, _ -> idx != i } },
                                        modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = newCheckItem, onValueChange = { newCheckItem = it },
                                    placeholder = { Text("Add checklist item…", fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f), singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                )
                                IconButton(
                                    onClick = {
                                        if (newCheckItem.isNotBlank()) {
                                            checklist = checklist + CheckItem(newCheckItem.trim())
                                            newCheckItem = ""
                                        }
                                    }
                                ) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) }
                            }
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp)

                    // ── Save Button ────────────────────────────────────────
                    Button(
                        onClick = { buildAndSaveTask() },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Task", fontWeight = FontWeight.SemiBold)
                    }
                }

                // ── AI TEXT ────────────────────────────────────────────────
                "ai_text" -> {
                    OutlinedTextField(
                        value = aiText, onValueChange = { aiText = it },
                        label = { Text("Describe your task") },
                        placeholder = { Text("e.g. Submit the project report by Friday 5pm") },
                        modifier = Modifier.fillMaxWidth(), maxLines = 4
                    )
                    Button(
                        onClick = { if (aiText.isNotBlank()) onAddFromText(aiText) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = aiText.isNotBlank() && !isAiLoading
                    ) {
                        if (isAiLoading) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) }
                        else Text("🤖 Create with AI")
                    }
                }

                // ── IMAGE OCR ──────────────────────────────────────────────
                "image" -> {
                    Text("Pick an image with text — AI will extract the task.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("Gallery")
                        }
                        Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text("Camera")
                        }
                    }
                }

                // ── VOICE ──────────────────────────────────────────────────
                "voice" -> {
                    Text("Hold mic, speak your task (at least 2 sec), tap again to send.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (tooShortWarning) {
                        Text("⚠️ Recording too short — speak for at least 2 seconds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                modifier = Modifier.size(80.dp)
                            ) {
                                Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                            Text(
                                if (isRecording) "🔴 ${recordingSeconds}s — tap to stop" else "Tap to start recording",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isRecording) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
