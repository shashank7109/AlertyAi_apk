package com.alertyai.app.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alertyai.app.data.model.Reminder
import com.alertyai.app.data.model.RepeatInterval
import com.alertyai.app.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(vm: RemindersViewModel = hiltViewModel()) {
    val reminders by vm.reminders.collectAsState()
    var showSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "NOTIFICATION CENTER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Reminders",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ClayButton(
                onClick = { showSheet = true },
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add reminder", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        if (reminders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(80.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.NotificationsNone, null, 
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), 
                                modifier = Modifier.size(40.dp))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("NO RECALLS ACTIVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to add a reminder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                item {
                    Text("Active Reminders", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(reminders, key = { it.id }) { reminder ->
                    ReminderItem(
                        reminder = reminder,
                        onDone   = { vm.markDone(reminder.id) },
                        onDelete = { vm.delete(reminder) }
                    )
                }
            }
        }
    }

    if (showSheet) {
        AddReminderSheet(
            onDismiss = { showSheet = false },
            onAdd     = { title, body, triggerAt, repeat ->
                vm.addReminder(title, body, triggerAt, repeat)
                showSheet = false
            }
        )
    }
}

@Composable
fun ReminderItem(reminder: Reminder, onDone: () -> Unit, onDelete: () -> Unit) {
    val fmt = SimpleDateFormat("d MMM, hh:mm a", Locale.getDefault())
    ClayCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                onClick = onDone,
                modifier = Modifier.size(32.dp).clip(CircleShape),
                color = if (reminder.isDone) Color(0xFF22C55E).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (reminder.isDone) Icons.Default.CheckCircle else Icons.Default.Notifications,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = if (reminder.isDone) Color(0xFF15803D) else MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Column(Modifier.weight(1f)) {
                Text(
                    reminder.title, 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.Medium,
                    color = if (reminder.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    fmt.format(Date(reminder.triggerAt)).uppercase(), 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (reminder.isRepeating) {
                    Text(
                        "REPEAT: ${reminder.repeatInterval.name.uppercase()}", 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderSheet(onDismiss: () -> Unit, onAdd: (String, String, Long, RepeatInterval) -> Unit) {
    var title  by remember { mutableStateOf("") }
    var body   by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf(RepeatInterval.NONE) }
    val defaultTime = System.currentTimeMillis() + 3_600_000L
    var triggerAt by remember { mutableStateOf(defaultTime) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column {
                Text("New Reminder", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                Text("Create Reminder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
            }

            ClayCard(shape = RoundedCornerShape(16.dp)) {
                TextField(
                    value = title, 
                    onValueChange = { title = it }, 
                    placeholder = { Text("Reminder title") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
            }

            ClayCard(shape = RoundedCornerShape(16.dp)) {
                TextField(
                    value = body, 
                    onValueChange = { body = it }, 
                    placeholder = { Text("DETAILED NOTES (OPTIONAL)") }, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("REPEAT ARCHITECTURE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeatInterval.values().forEach { r ->
                        Surface(
                            onClick = { repeat = r },
                            shape = RoundedCornerShape(12.dp),
                            color = if (repeat == r) MaterialTheme.colorScheme.primary else Color.Transparent,
                            border = if (repeat == r) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    r.name.uppercase(), 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Medium,
                                    color = if (repeat == r) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            ClayButton(
                onClick  = { onAdd(title, body, triggerAt, repeat) },
                enabled  = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(64.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) { 
                Text("Create Reminder", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall) 
            }
        }
    }
}
