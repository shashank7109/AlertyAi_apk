package com.alertyai.app.ui.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alertyai.app.data.model.Task
import java.util.Calendar

/**
 * Hourly timeline view for the "TODAY / DAILY" filter.
 * Tasks with a dueTime are placed in their hour slot.
 * Tasks without a time appear in the "All Day" section.
 */
@Composable
fun DailyTimelineView(
    tasks: List<Task>,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group tasks into hour buckets (0-23) and "all day"
    val tasksByHour = mutableMapOf<Int, MutableList<Task>>()
    val allDayTasks = mutableListOf<Task>()

    tasks.forEach { task ->
        val hour = task.dueTime?.let { millis ->
            Calendar.getInstance().apply { timeInMillis = millis }
                .get(Calendar.HOUR_OF_DAY)
        }
        if (hour != null) {
            tasksByHour.getOrPut(hour) { mutableListOf() }.add(task)
        } else {
            allDayTasks.add(task)
        }
    }

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // All Day section
        if (allDayTasks.isNotEmpty()) {
            item {
                Text(
                    "ALL DAY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
            items(allDayTasks, key = { "ad_${it.id}" }) { task ->
                TimelineTaskCard(task = task, onToggle = { onToggle(task) }, onEdit = { onEdit(task) })
                Spacer(Modifier.height(8.dp))
            }
        }

        // Hour slots: show hours 6am to 11pm; highlight current hour
        items((6..23).toList(), key = { "hour_$it" }) { hour ->
            val hourTasks = tasksByHour[hour] ?: emptyList()
            val isCurrentHour = hour == currentHour
            val label = when {
                hour == 0 -> "12 AM"
                hour < 12 -> "$hour AM"
                hour == 12 -> "12 PM"
                else -> "${hour - 12} PM"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Time label
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isCurrentHour) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrentHour)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .width(52.dp)
                        .padding(top = 10.dp)
                )

                // Vertical line
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    // Dot on the timeline
                    Box(
                        modifier = Modifier
                            .size(if (isCurrentHour) 10.dp else 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isCurrentHour)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                    // Line segment
                    if (hour < 23) {
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .width(1.dp)
                                .height(if (hourTasks.isEmpty()) 40.dp else (50 * hourTasks.size).dp)
                        )
                    }
                }

                // Tasks in this slot
                Column(modifier = Modifier.weight(1f)) {
                    if (hourTasks.isEmpty()) {
                        Spacer(Modifier.height(36.dp))
                    } else {
                        hourTasks.forEach { task ->
                            TimelineTaskCard(
                                task = task,
                                onToggle = { onToggle(task) },
                                onEdit = { onEdit(task) }
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun TimelineTaskCard(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val isDone = task.isDone
    val priorityColor = when (task.priority.name) {
        "HIGH", "URGENT" -> Color(0xFFEF4444)
        "MEDIUM" -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Surface(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isDone)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surface,
        tonalElevation = if (isDone) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Priority indicator bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDone) Color.Gray.copy(0.3f) else priorityColor)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDone)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (!task.note.isNullOrBlank()) {
                    Text(
                        task.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Recurring badge
            if (task.repeatInterval != com.alertyai.app.data.model.RepeatInterval.NONE) {
                Text(
                    "🔄",
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Checkbox(
                checked = isDone,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
