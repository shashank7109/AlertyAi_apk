package com.alertyai.app.ui.home

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alertyai.app.network.TokenManager
import com.alertyai.app.ui.reminders.RemindersViewModel
import com.alertyai.app.ui.tasks.TasksViewModel
import com.alertyai.app.ui.tasks.TaskItem
import com.alertyai.app.ui.tasks.AddTaskSheet
import com.alertyai.app.ui.components.ClayCard
import com.alertyai.app.ui.components.ClayButton
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    tasksVm: TasksViewModel = hiltViewModel(),
    remindersVm: RemindersViewModel = hiltViewModel(),
    onNavigateToTasks: () -> Unit = {},
    onNavigateToTeams: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val tasks by tasksVm.tasks.collectAsState()
    val upcoming by remindersVm.upcoming.collectAsState()
    val aiState by tasksVm.aiState.collectAsState()
    val isLoggedIn = remember { TokenManager.isLoggedIn(context) }
    var showAddSheet by remember { mutableStateOf<String?>(null) }
    
    val todayTasks = tasks.filter { !it.isDone }.take(3)
    val today = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())

    // Handle AI status messages
    LaunchedEffect(aiState.aiSuccess, aiState.aiError) {
        aiState.aiSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            tasksVm.clearAiMessages()
        }
        aiState.aiError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            tasksVm.clearAiMessages()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp)
    ) {
        // Header Section (Clean and Minimal)
        item {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = today.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Day",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-1).sp
                    )
                    Surface(
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = onNavigateToProfile
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Quick Actions (Glassmorphism layout)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Mic,
                    label = "Voice",
                    onClick = { showAddSheet = "voice" }
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PhotoCamera,
                    label = "Scan",
                    onClick = { showAddSheet = "image" }
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Add,
                    label = "New",
                    onClick = { showAddSheet = "manual" }
                )
            }
        }

        // Status Summary
        item {
            ClayCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatusIndicator(count = todayTasks.size, label = "Active", onClick = onNavigateToTasks)
                    StatusIndicator(count = upcoming.size, label = "Upcoming", onClick = onNavigateToReminders)
                    StatusIndicator(count = tasks.filter { it.isDone }.size, label = "Done", onClick = onNavigateToTasks)
                }
            }
        }

        // Today's Missions
        if (todayTasks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Recent Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
            }
            items(todayTasks, key = { it.id }) { task ->
                TaskItem(
                    task     = task,
                    onToggle = { tasksVm.toggleDone(context, task) },
                    onDelete = { tasksVm.deleteTask(context, task) }
                )
            }
            
            item {
                Spacer(Modifier.height(8.dp))
                ClayButton(
                    onClick = onNavigateToTasks,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text("View All Tasks", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Empty state
        if (todayTasks.isEmpty() && upcoming.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("All clear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                        Text("You have no pending tasks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    showAddSheet?.let { mode ->
        AddTaskSheet(
            isLoggedIn = isLoggedIn,
            isAiLoading = aiState.isAiLoading,
            onDismiss = { showAddSheet = null },
            onAddTask = { task -> tasksVm.addFullTask(context, task) },
            onUpdateTask = { task -> tasksVm.updateTask(context, task) },
            onAddFromText = { text ->
                tasksVm.createTaskFromText(context, text)
                showAddSheet = null
            },
            onImageSelected = { uri ->
                tasksVm.createTaskFromImage(context, uri)
                showAddSheet = null
            },
            onVoiceFile = { file ->
                tasksVm.createTaskFromVoice(context, file)
                showAddSheet = null
            },
            initialMode = mode
        )
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {}
) {
    ClayCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = label, 
                style = MaterialTheme.typography.bodySmall, 
                fontWeight = FontWeight.Medium, 
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatusIndicator(count: Int, label: String, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
