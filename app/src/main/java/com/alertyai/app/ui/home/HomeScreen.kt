package com.alertyai.app.ui.home

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.alertyai.app.ui.reminders.RemindersViewModel
import com.alertyai.app.ui.tasks.TasksViewModel
import com.alertyai.app.ui.tasks.TaskItem
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val tasks by tasksVm.tasks.collectAsState()
    val upcoming by remindersVm.upcoming.collectAsState()
    val todayTasks = tasks.filter { !it.isDone }.take(3)

    val today = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 32.dp, bottom = 40.dp)
    ) {
        // Header Section
        item {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = today.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CORE SYSTEM",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                    Surface(
                        modifier = Modifier.size(48.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        onClick = onNavigateToProfile
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("A", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Quick Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Mic,
                    label = "VOICE",
                    containerColor = MaterialTheme.colorScheme.primary
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PhotoCamera,
                    label = "SCAN",
                    containerColor = MaterialTheme.colorScheme.secondary
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Add,
                    label = "NEW MISSION",
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToTasks
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
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatusIndicator(count = todayTasks.size, label = "ACTIVE", onClick = onNavigateToTasks)
                    StatusIndicator(count = upcoming.size, label = "SIGNAL", onClick = onNavigateToReminders)
                    StatusIndicator(count = tasks.filter { it.isDone }.size, label = "CLOSED", onClick = onNavigateToTasks)
                }
            }
        }

        // Today's Missions
        if (todayTasks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "MISSION LOG",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(todayTasks, key = { it.id }) { task ->
                TaskItem(
                    task     = task,
                    onToggle = { tasksVm.toggleDone(context, task) },
                    onDelete = { tasksVm.deleteTask(context, task) }
                )
            }
            
            item {
                ClayButton(
                    onClick = onNavigateToTasks,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("VIEW ALL MISSIONS", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Empty state
        if (todayTasks.isEmpty() && upcoming.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("V", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("ALL CLEAR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                        Text("SYSTEM OPERATIONAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    containerColor: Color,
    onClick: () -> Unit = {}
) {
    ClayCard(
        modifier = modifier,
        containerColor = containerColor.copy(alpha = 0.12f),
        onClick = onClick
    ) {
        Icon(icon, contentDescription = null, tint = containerColor, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            fontWeight = FontWeight.Black, 
            color = containerColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
