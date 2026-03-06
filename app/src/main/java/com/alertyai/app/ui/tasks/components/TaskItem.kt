package com.alertyai.app.ui.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.alertyai.app.data.model.CheckItem
import com.alertyai.app.data.model.Priority
import com.alertyai.app.data.model.Task
import com.alertyai.app.ui.components.ClayCard
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                    fontWeight = FontWeight.Medium,
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
