package com.alertyai.app.ui.tasks

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alertyai.app.data.model.CheckItem
import com.alertyai.app.data.model.Priority
import com.alertyai.app.data.model.Task
import com.alertyai.app.network.TokenManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen() {
    val context = LocalContext.current
    val vm: TasksViewModel = hiltViewModel()
    val tasks by vm.tasks.collectAsState()
    val aiState by vm.aiState.collectAsState()
    val isLoggedIn = remember { TokenManager.isLoggedIn(context) }

    var showAddSheet by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("all") }

    // Sync backend tasks (created via Chat/AI) into local Room DB on screen open
    LaunchedEffect(Unit) {
        vm.syncFromBackend(context)
    }

    // Auto-clear AI status messages after 4 seconds
    LaunchedEffect(aiState.aiSuccess, aiState.aiError) {
        if (aiState.aiSuccess != null || aiState.aiError != null) {
            delay(4000)
            vm.clearAiMessages()
        }
    }

    val filteredTasks = remember(tasks, filter) {
        when (filter) {
            "today" -> tasks.filter { task ->
                task.dueDate?.let { date ->
                    val today = Calendar.getInstance()
                    val taskCal = Calendar.getInstance().apply { timeInMillis = date }
                    today.get(Calendar.DAY_OF_YEAR) == taskCal.get(Calendar.DAY_OF_YEAR) &&
                    today.get(Calendar.YEAR) == taskCal.get(Calendar.YEAR)
                } ?: false
            }
            "upcoming" -> tasks.filter {
                it.dueDate != null && it.dueDate > System.currentTimeMillis()
            }
            else -> tasks
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tasks", fontWeight = FontWeight.SemiBold) })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add Task", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── AI Status Banner ──────────────────────────────────────────────
            AnimatedVisibility(visible = aiState.aiSuccess != null || aiState.aiError != null || aiState.isAiLoading) {
                Surface(color = when {
                    aiState.isAiLoading -> MaterialTheme.colorScheme.secondaryContainer
                    aiState.aiError != null -> MaterialTheme.colorScheme.errorContainer
                    else -> Color(0xFFDCFCE7)
                }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (aiState.isAiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("AI is processing…", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(aiState.aiSuccess ?: aiState.aiError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (aiState.aiError != null) MaterialTheme.colorScheme.onErrorContainer
                                        else Color(0xFF166534))
                        }
                    }
                }
            }

            // ── Filter Chips ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "All", "today" to "Today", "upcoming" to "Upcoming").forEach { (key, label) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            // ── Task List ─────────────────────────────────────────────────────
            if (filteredTasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No tasks", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (filter == "today") "No tasks due today" else "Tap + to add one",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onToggle = { vm.toggleDone(task) },
                            onDelete = { vm.deleteTask(task) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddSheet) {
        AddTaskSheet(
            isLoggedIn = isLoggedIn,
            isAiLoading = aiState.isAiLoading,
            onDismiss = { showAddSheet = false },
            onAddTask = { task -> vm.addFullTask(task) },
            onAddFromText = { text ->
                vm.createTaskFromText(context, text)
                showAddSheet = false
            },
            onImageSelected = { uri ->
                vm.createTaskFromImage(context, uri)
                showAddSheet = false
            },
            onVoiceFile = { file ->
                vm.createTaskFromVoice(context, file)
                showAddSheet = false
            }
        )
    }
}

// ── Task Item card — also used by HomeScreen ──────────────────────────────────
@Composable
internal fun TaskItem(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    val priorityColor = when (task.priority) {
        Priority.HIGH   -> Color(0xFFEF4444)
        Priority.NORMAL -> Color(0xFFF97316)
        Priority.LOW    -> Color(0xFF22C55E)
    }

    val gson = remember { Gson() }
    val checklist = remember(task.checklistJson) {
        try { gson.fromJson<List<CheckItem>>(task.checklistJson, object : TypeToken<List<CheckItem>>() {}.type) ?: emptyList() }
        catch (_: Exception) { emptyList() }
    }
    val subtasks = remember(task.subtasksJson) {
        try { gson.fromJson<List<String>>(task.subtasksJson, object : TypeToken<List<String>>() {}.type) ?: emptyList() }
        catch (_: Exception) { emptyList() }
    }
    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(priorityColor))
                Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
                Column(Modifier.weight(1f)) {
                    Text(
                        task.title,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                        color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface
                    )
                    if (task.note.isNotBlank()) {
                        Text(task.note, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    // Date / time / alarm row
                    if (task.dueDate != null || task.dueTime != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            task.dueDate?.let {
                                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Text(dateFormat.format(Date(it)), fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            task.dueTime?.let {
                                Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                                Text(timeFormat.format(Date(it)), fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            if (task.alarmEnabled) {
                                Icon(Icons.Default.Alarm, null, modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                    // Location
                    if (task.location.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(task.location, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    // Checklist progress
                    if (checklist.isNotEmpty()) {
                        val done = checklist.count { it.done }
                        Text("☑ $done/${checklist.size} done", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // Subtask count
                    if (subtasks.isNotEmpty()) {
                        Text("⤷ ${subtasks.size} subtask${if (subtasks.size > 1) "s" else ""}",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
