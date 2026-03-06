package com.alertyai.app.ui.teams.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alertyai.app.network.TeamTask

@Composable
fun TaskPanelDialog(
    isAdmin: Boolean,
    myTasks: List<TeamTask>,
    assignedByMeTasks: List<TeamTask>,
    onDismiss: () -> Unit
) {
    val tasks = if (isAdmin) assignedByMeTasks else myTasks
    val title = if (isAdmin) "TASKS I ASSIGNED" else "MY ASSIGNED TASKS"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                if (tasks.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("NO TASKS YET", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tasks) { task ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(task.title.uppercase(), style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                        val statusColor = when (task.status) {
                                            "completed" -> Color(0xFF22C55E)
                                            "in_progress" -> Color(0xFFF97316)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                        Text(task.status.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Medium,
                                            color = statusColor, modifier = Modifier
                                                .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp))
                                    }
                                    if (isAdmin) {
                                        Text("→ ${task.assigneeEmail}", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                    } else {
                                        Text("From: ${task.assignedByEmail}", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}
