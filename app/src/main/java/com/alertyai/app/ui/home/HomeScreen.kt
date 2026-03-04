package com.alertyai.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alertyai.app.ui.reminders.RemindersViewModel
import com.alertyai.app.ui.tasks.TasksViewModel
import com.alertyai.app.ui.tasks.TaskItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    tasksVm: TasksViewModel = hiltViewModel(),
    remindersVm: RemindersViewModel = hiltViewModel()
) {
    val tasks by tasksVm.tasks.collectAsState()
    val upcoming by remindersVm.upcoming.collectAsState()
    val todayTasks = tasks.filter { !it.isDone }.take(5)

    val today = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp)
    ) {
        // Header
        item {
            Text(
                text = today,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "AlertyAI",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }

        // Summary chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryChip("${todayTasks.size} tasks", MaterialTheme.colorScheme.primary)
                SummaryChip("${upcoming.size} reminders", MaterialTheme.colorScheme.secondary)
            }
        }

        // Today's Tasks
        if (todayTasks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Today's Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(todayTasks, key = { it.id }) { task ->
                TaskItem(
                    task     = task,
                    onToggle = { tasksVm.toggleDone(task) },
                    onDelete = { tasksVm.deleteTask(task) }
                )
            }
        }

        // Upcoming reminders
        if (upcoming.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Upcoming Reminders",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(upcoming.take(3), key = { it.id }) { reminder ->
                Card(
                    shape  = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(reminder.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            if (reminder.body.isNotEmpty())
                                Text(reminder.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(reminder.triggerAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Empty state
        if (todayTasks.isEmpty() && upcoming.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(12.dp))
                        Text("All clear for today!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape  = MaterialTheme.shapes.extraLarge,
        color  = color.copy(alpha = 0.12f),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
