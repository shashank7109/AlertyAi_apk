package com.alertyai.app.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.alertyai.app.ui.components.ClayCard
import com.alertyai.app.ui.components.ClayButton
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
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var filter by remember { mutableStateOf("all") }

    LaunchedEffect(Unit) {
        vm.syncFromBackend(context)
    }

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text(
                    "MISSION CONTROL", 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ACTIVE TASKS",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, "Add Task", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        floatingActionButton = {
            ClayButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("NEW TASK", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // AI Status Banner
            AnimatedVisibility(visible = aiState.aiSuccess != null || aiState.aiError != null || aiState.isAiLoading) {
                Surface(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        aiState.isAiLoading -> MaterialTheme.colorScheme.secondaryContainer
                        aiState.aiError != null -> MaterialTheme.colorScheme.errorContainer
                        else -> Color(0xFFDCFCE7).copy(alpha = 0.5f)
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (aiState.isAiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("NEURAL PROCESSING...", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Text((aiState.aiSuccess ?: aiState.aiError ?: "").uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (aiState.aiError != null) MaterialTheme.colorScheme.error
                                        else Color(0xFF166534))
                        }
                    }
                }
            }

            // Filter Chips 
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("all" to "ALL", "today" to "TODAY", "upcoming" to "INCOMING").forEach { (key, label) ->
                    val isActive = filter == key
                    ClayCard(
                        onClick = { filter = key },
                        containerColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        elevation = if (isActive) 0.dp else 2.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            label, 
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Task List 
            if (filteredTasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("0", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("QUIET SECTOR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text("NO ACTIVE SIGNALS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onToggle = { vm.toggleDone(context, task) },
                            onDelete = { vm.deleteTask(context, task) },
                            onEdit   = { editingTask = task }
                        )
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }

    if (showAddSheet) {
        AddTaskSheet(
            isLoggedIn = isLoggedIn,
            isAiLoading = aiState.isAiLoading,
            onDismiss = { showAddSheet = false },
            onAddTask = { task -> vm.addFullTask(context, task) },
            onUpdateTask = { task -> vm.updateTask(context, task) },
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

    editingTask?.let { task ->
        AddTaskSheet(
            isLoggedIn = isLoggedIn,
            isAiLoading = false,
            onDismiss = { editingTask = null },
            onAddTask = { },
            onUpdateTask = { updated ->
                vm.updateTask(context, updated)
                editingTask = null
            },
            onAddFromText = { },
            onImageSelected = { },
            onVoiceFile = { },
            existingTask = task
        )
    }
}

@Composable
fun TaskItem(task: Task, onToggle: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit = {}) {
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
    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    ClayCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (task.isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                        else MaterialTheme.colorScheme.surface,
        elevation = if (task.isDone) 2.dp else 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (task.isDone) Color(0xFF10B981) else priorityColor.copy(alpha = 0.1f))
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (task.isDone) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = Color.White)
                } else {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(priorityColor))
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    task.title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.onSurface
                )
                
                if (task.note.isNotBlank()) {
                    Text(
                        task.note, 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (task.dueDate != null || task.dueTime != null || checklist.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        task.dueDate?.let {
                            Indicator(Icons.Default.CalendarToday, dateFormat.format(Date(it)))
                        }
                        task.dueTime?.let {
                            Indicator(Icons.Default.AccessTime, timeFormat.format(Date(it)))
                        }
                        if (checklist.isNotEmpty()) {
                            val done = checklist.count { it.done }
                            Indicator(Icons.Default.FactCheck, "$done/${checklist.size}")
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun Indicator(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}
