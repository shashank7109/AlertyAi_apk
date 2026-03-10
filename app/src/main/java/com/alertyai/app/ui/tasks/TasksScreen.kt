package com.alertyai.app.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import com.alertyai.app.ui.tasks.components.DailyTimelineView
import com.alertyai.app.ui.tasks.components.TaskFilters
import com.alertyai.app.ui.tasks.components.TaskItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(autoStartVoice: Boolean = false, onAutoStartVoiceConsumed: () -> Unit = {}) {
    val context = LocalContext.current
    val vm: TasksViewModel = hiltViewModel()
    val tasks by vm.tasks.collectAsState()
    val aiState by vm.aiState.collectAsState()
    val isLoggedIn = remember { TokenManager.isLoggedIn(context) }

    var showAddSheet by remember { mutableStateOf(autoStartVoice) }
    
    // React to new intents when screen is already composed
    LaunchedEffect(autoStartVoice) {
        if (autoStartVoice) {
            showAddSheet = true
            onAutoStartVoiceConsumed()
        }
    }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var filter by remember { mutableStateOf("all") }

    // Sync tasks when screen becomes visible
    LaunchedEffect(Unit) {
        vm.syncFromBackend(context)
    }
    
    // Also sync when screen is recomposed (e.g., when navigating back to it)
    DisposableEffect(Unit) {
        vm.syncFromBackend(context)
        onDispose { }
    }

    LaunchedEffect(aiState.aiSuccess, aiState.aiError) {
        if (aiState.aiSuccess != null || aiState.aiError != null) {
            delay(4000)
            vm.clearAiMessages()
        }
    }

    var currentFilter by remember { mutableStateOf("ALL") } // ALL, PAST, TODAY, WEEKLY, MONTHLY, CUSTOM_DATE
    var pastTasksFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, COMPLETED
    var selectedDate by remember { mutableStateOf<Long?>(null) } // Used for CUSTOM_DATE

    val filteredTasks = remember(tasks, currentFilter, selectedDate, pastTasksFilter) {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = todayCal.timeInMillis
        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        tasks.filter { task ->
            val isPast = task.dueDate != null && task.dueDate < startOfToday

            when (currentFilter) {
                "PAST" -> {
                    val matchesStatus = when (pastTasksFilter) {
                        "PENDING" -> !task.isDone
                        "COMPLETED" -> task.isDone
                        else -> true
                    }
                    isPast && matchesStatus
                }
                "TODAY" -> {
                    val isToday = task.dueDate != null && task.dueDate >= startOfToday && task.dueDate < startOfToday + 86400000L
                    val isDaily = task.repeatInterval == com.alertyai.app.data.model.RepeatInterval.DAILY
                    isToday || isDaily || (isPast && !task.isDone) // Carried over past pending
                }
                "WEEKLY" -> {
                    val isThisWeek = task.dueDate != null && task.dueDate >= startOfWeek && task.dueDate < startOfWeek + 7 * 86400000L
                    val isWeekly = task.repeatInterval == com.alertyai.app.data.model.RepeatInterval.WEEKLY
                    isThisWeek || isWeekly || (isPast && !task.isDone)
                }
                "MONTHLY" -> {
                    val isThisMonth = task.dueDate != null && task.dueDate >= startOfMonth && task.dueDate < startOfMonth + 31 * 86400000L // Approx
                    val isMonthly = task.repeatInterval == com.alertyai.app.data.model.RepeatInterval.MONTHLY
                    isThisMonth || isMonthly || (isPast && !task.isDone)
                }
                "CUSTOM_DATE" -> {
                    selectedDate?.let { sd ->
                        val selCal = Calendar.getInstance().apply { timeInMillis = sd }
                        val selYear = selCal.get(Calendar.YEAR)
                        val selDay = selCal.get(Calendar.DAY_OF_YEAR)
                        val taskCal = task.dueDate?.let { Calendar.getInstance().apply { timeInMillis = it } }
                        
                        val matchesDate = taskCal?.let {
                            it.get(Calendar.YEAR) == selYear && it.get(Calendar.DAY_OF_YEAR) == selDay
                        } ?: false

                        matchesDate
                    } ?: false
                }
                else -> { // "ALL"
                    true
                }
            }
        }.sortedBy { it.dueDate ?: Long.MAX_VALUE }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text(
                    "Tasks", 
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
                        "My Tasks",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
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
                Text("NEW TASK", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelSmall)
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
                            Text("Processing...", style = MaterialTheme.typography.labelSmall)
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
            val datePickerDialog = remember {
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val cal = Calendar.getInstance()
                        cal.set(year, month, dayOfMonth, 0, 0, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        selectedDate = cal.timeInMillis
                    },
                    Calendar.getInstance().get(Calendar.YEAR),
                    Calendar.getInstance().get(Calendar.MONTH),
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                )
            }

            TaskFilters(
                datePickerDialog = datePickerDialog,
                selectedDate = selectedDate,
                onSelectedDateChange = { 
                    selectedDate = it
                    if (it != null) currentFilter = "CUSTOM_DATE"
                },
                currentFilter = currentFilter,
                onFilterChange = { currentFilter = it },
                pastTasksFilter = pastTasksFilter,
                onPastTasksFilterChange = { pastTasksFilter = it }
            )

            // Task List — show timeline view for TODAY, flat list for everything else
            if (currentFilter == "TODAY" && filteredTasks.isNotEmpty()) {
                DailyTimelineView(
                    tasks = filteredTasks,
                    onToggle = { task -> vm.toggleDone(context, task) },
                    onEdit = { task -> editingTask = task },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (filteredTasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("0", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("No tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Text("All done for now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            onEdit   = { 
                                editingTask = task
                            }
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
            autoStartVoice = autoStartVoice,
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


