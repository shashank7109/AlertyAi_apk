package com.alertyai.app.ui.reminders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alertyai.app.data.model.Reminder
import com.alertyai.app.data.model.RepeatInterval
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
                title = { Text("Reminders", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = Color.White,
                shape          = MaterialTheme.shapes.extraLarge
            ) { Icon(Icons.Default.Add, "Add reminder") }
        }
    ) { padding ->
        if (reminders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔔", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(12.dp))
                    Text("No reminders", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tap + to schedule one", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
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
    Card(
        shape  = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isDone) MaterialTheme.colorScheme.surfaceVariant
                             else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onDone, modifier = Modifier.size(28.dp)) {
                Icon(
                    if (reminder.isDone) Icons.Default.CheckCircle else Icons.Default.Notifications,
                    contentDescription = "Done",
                    tint = if (reminder.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(Modifier.weight(1f)) {
                Text(reminder.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(fmt.format(Date(reminder.triggerAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                if (reminder.isRepeating) {
                    Text("Repeats: ${reminder.repeatInterval.name.lowercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    // Default to 1 hour from now
    val defaultTime = System.currentTimeMillis() + 3_600_000L
    var triggerAt by remember { mutableStateOf(defaultTime) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("New Reminder", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.large)
            OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large)

            Text("Repeat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatInterval.values().forEach { r ->
                    FilterChip(selected = repeat == r, onClick = { repeat = r }, label = { Text(r.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }

            Button(
                onClick  = { onAdd(title, body, triggerAt, repeat) },
                enabled  = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = MaterialTheme.shapes.extraLarge
            ) { Text("Schedule Reminder") }
        }
    }
}

// Suppress missing import warning - Color comes from material3 defaults
private val Color = androidx.compose.ui.graphics.Color
