package com.alertyai.app.ui.teams.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alertyai.app.network.MentionMember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignTaskDialog(
    members: List<MentionMember>,
    success: String?,
    error: String?,
    onAssign: (targets: List<MentionMember>, title: String, desc: String, priority: String, deadline: String?, freq: String, rTime: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") }
    var dueDateTime by remember { mutableStateOf<Long?>(null) }
    var reminderFrequency by remember { mutableStateOf("daily") }
    
    // Multiple selection state
    val selectedMembers = remember { mutableStateListOf<MentionMember>() }
    val context = LocalContext.current
    val isoFormatter = remember { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ASSIGN TASK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    if (success != null) {
                        Text(success.uppercase(), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                    if (error != null) {
                        Text(error, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                    }
                }
                
                item {
                    Text("ASSIGNEES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                items(members) { member ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (selectedMembers.contains(member)) selectedMembers.remove(member)
                            else selectedMembers.add(member)
                        }
                    ) {
                        Checkbox(
                            checked = selectedMembers.contains(member),
                            onCheckedChange = { checked ->
                                if (checked) selectedMembers.add(member)
                                else selectedMembers.remove(member)
                            }
                        )
                        Text(member.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                item {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text("TASK TITLE") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        label = { Text("DESCRIPTION (OPTIONAL)") },
                        modifier = Modifier.fillMaxWidth(), maxLines = 3
                    )
                }
                
                item {
                    val cal = java.util.Calendar.getInstance()
                    val dateLabel = dueDateTime?.let { 
                        java.text.SimpleDateFormat("d MMM yyyy, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(it)).uppercase() 
                    } ?: "SELECT DEADLINE"
                    
                    Surface(
                        onClick = {
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val c = java.util.Calendar.getInstance().apply { set(y, m, d) }
                                    android.app.TimePickerDialog(
                                        context,
                                        { _, h, min ->
                                            c.set(java.util.Calendar.HOUR_OF_DAY, h)
                                            c.set(java.util.Calendar.MINUTE, min)
                                            c.set(java.util.Calendar.SECOND, 0)
                                            dueDateTime = c.timeInMillis
                                        },
                                        cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), false
                                    ).show()
                                },
                                cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(dateLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                item {
                    Text("PRIORITY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("low" to Color(0xFF22C55E), "normal" to Color(0xFFF97316), "high" to Color(0xFFEF4444)).forEach { (p, color) ->
                            val selected = priority == p
                            Surface(
                                onClick = { priority = p },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(p.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Medium,
                                    color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                            }
                        }
                    }
                }
                
                item {
                    Text("REMINDER FREQUENCY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("none", "daily", "weekly").forEach { freq ->
                            val selected = reminderFrequency == freq
                            Surface(
                                onClick = { reminderFrequency = freq },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha=0.15f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(freq.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank() && selectedMembers.isNotEmpty()) {
                    val formattedDate = dueDateTime?.let { isoFormatter.format(java.util.Date(it)) }
                    // Default reminder time to 9 AM UTC for now if due date provided
                    val rTime = if (reminderFrequency != "none") "09:00" else null 
                    onAssign(selectedMembers.toList(), title, description, priority, formattedDate, reminderFrequency, rTime)
                }
            }, enabled = title.isNotBlank() && selectedMembers.isNotEmpty()) {
                Text("Assign Tasks", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelSmall)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
