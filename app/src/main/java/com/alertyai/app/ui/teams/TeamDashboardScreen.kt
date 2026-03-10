package com.alertyai.app.ui.teams

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alertyai.app.data.model.TeamDetailedResponse
import com.alertyai.app.data.model.TeamTask
import com.alertyai.app.ui.components.ClayCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDashboardScreen(
    teamId: String,
    teamName: String,
    onBack: () -> Unit,
    onNavigateToChat: () -> Unit,
    viewModel: TeamDashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(teamId) {
        viewModel.loadTeamDetails(context, teamId)
    }

    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chat", "Tasks", "Members", "Analytics")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(teamName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    if (state.teamDetails?.isAdmin == true) {
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Team", tint = MaterialTheme.colorScheme.error)
                        }
                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("Delete Team") },
                                text = { Text("Are you sure you want to delete this team? This action cannot be undone.") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showDeleteConfirm = false
                                        viewModel.deleteTeam(context, teamId) {
                                            onBack()
                                        }
                                    }) {
                                        Text("DELETE", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                        Text("CANCEL")
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (state.isLoading && state.teamDetails == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.error != null && state.teamDetails == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                }
            } else {
                state.teamDetails?.let { team ->
                    // Header Section
                    TeamDashboardHeader(team)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats Section
                    TeamDashboardStats(team)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tabs
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                                height = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = activeTab == index,
                                onClick = {
                                    if (index == 0) {
                                        // On Android, we currently navigate to TeamChatScreen separately
                                        // rather than embedding it, because TeamChatScreen has its own complex Scaffold
                                        // But replicating the WebApp precisely means we should embed it, or navigate to it.
                                        // The prompt says: "The Chat tab will embed the existing TeamChatScreen functionality (or route to it)"
                                        onNavigateToChat()
                                    } else {
                                        activeTab = index
                                    }
                                },
                                text = {
                                    Text(
                                        title.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (activeTab == index) FontWeight.Black else FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }

                    // Content Section
                    Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                        when (activeTab) {
                            1 -> TasksTabContent(
                                tasks = team.tasks,
                                isAdmin = team.isAdmin,
                                members = team.members,
                                onAssignTask = { title, desc, priority, assignedTo ->
                                    viewModel.assignTask(context, teamId, title, desc, priority, assignedTo)
                                }
                            )
                            2 -> MembersTabContent(
                                members = team.members,
                                isAdmin = team.isAdmin,
                                joinCode = team.joinCode,
                                onMakeAdmin = { userId -> viewModel.updateMemberRole(context, teamId, userId, "admin") },
                                onMakeMember = { userId -> viewModel.updateMemberRole(context, teamId, userId, "member") },
                                onRemoveMember = { userId -> viewModel.removeMember(context, teamId, userId) }
                            )
                            3 -> AnalyticsTabContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamDashboardHeader(team: TeamDetailedResponse) {
    ClayCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = team.name.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                // Leader Badge
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "LEADER",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            if (!team.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = team.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${team.members.size} MEMBERS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
                val completedTasks = team.tasks.count { it.status == "completed" }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$completedTasks DONE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun TeamDashboardStats(team: TeamDetailedResponse) {
    val total = team.tasks.size
    val inProgress = team.tasks.count { it.status == "in_progress" || it.status == "accepted" }
    val done = team.tasks.count { it.status == "completed" }
    val pending = team.tasks.count { it.status == "pending" }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(modifier = Modifier.weight(1f), label = "TOTAL", value = total.toString(), icon = Icons.Default.Assignment, color = Color(0xFF3B82F6))
        StatCard(modifier = Modifier.weight(1f), label = "ACTIVE", value = inProgress.toString(), icon = Icons.Default.Schedule, color = Color(0xFFF59E0B))
        StatCard(modifier = Modifier.weight(1f), label = "DONE", value = done.toString(), icon = Icons.Default.CheckCircle, color = Color(0xFF10B981))
        StatCard(modifier = Modifier.weight(1f), label = "PENDING", value = pending.toString(), icon = Icons.Default.Pending, color = Color(0xFFEF4444))
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    ClayCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.Start) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksTabContent(
    tasks: List<TeamTask>,
    isAdmin: Boolean = false,
    members: List<com.alertyai.app.data.model.OrgMember> = emptyList(),
    onAssignTask: (String, String, String, String) -> Unit = { _, _, _, _ -> }
) {
    var filter by remember { mutableStateOf("all") }
    val filteredTasks = if (filter == "all") tasks else tasks.filter { it.status == filter }

    var showAssignDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            listOf("all", "pending", "in_progress", "completed").forEach { f ->
                val selected = filter == f
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { filter = f }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        f.replace('_', ' ').uppercase(),
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (filteredTasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tasks found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredTasks, key = { it.id }) { task ->
                    TeamTaskItemCard(task)
                }
            }
            }
        }
        
        if (isAdmin) {
            FloatingActionButton(
                onClick = { showAssignDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Assign Task")
            }
        }
    } // Ends the Box

    if (showAssignDialog) {
        var newTaskTitle by remember { mutableStateOf("") }
        var newTaskDesc by remember { mutableStateOf("") }
        var selectedPriority by remember { mutableStateOf("medium") }
        var selectedAssignee by remember { mutableStateOf(members.firstOrNull()?.userId ?: "") }
        var expandedPriority by remember { mutableStateOf(false) }
        var expandedAssignee by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text("Assign a Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTaskDesc,
                        onValueChange = { newTaskDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Priority Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedPriority,
                        onExpandedChange = { expandedPriority = !expandedPriority },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedPriority.uppercase(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Priority") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPriority) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPriority,
                            onDismissRequest = { expandedPriority = false }
                        ) {
                            listOf("low", "medium", "high").forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.uppercase()) },
                                    onClick = {
                                        selectedPriority = p
                                        expandedPriority = false
                                    }
                                )
                            }
                        }
                    }

                    // Assignee Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedAssignee,
                        onExpandedChange = { expandedAssignee = !expandedAssignee },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = members.find { it.userId == selectedAssignee }?.displayName ?: "Select Member",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assign To") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAssignee) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedAssignee,
                            onDismissRequest = { expandedAssignee = false }
                        ) {
                            members.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.displayName) },
                                    onClick = {
                                        selectedAssignee = m.userId
                                        expandedAssignee = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank() && selectedAssignee.isNotBlank()) {
                            onAssignTask(newTaskTitle, newTaskDesc, selectedPriority, selectedAssignee)
                            showAssignDialog = false
                        }
                    }
                ) {
                    Text("ASSIGN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssignDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun TeamTaskItemCard(task: TeamTask) {
    ClayCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (!task.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(task.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(task.status.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(task.priority.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

@Composable
fun MembersTabContent(
    members: List<com.alertyai.app.data.model.OrgMember>,
    isAdmin: Boolean,
    joinCode: String?,
    onMakeAdmin: (String) -> Unit,
    onMakeMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!joinCode.isNullOrBlank()) {
            item {
                ClayCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Team Join Code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Share this code to let others join.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clickable {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(joinCode))
                                    android.widget.Toast.makeText(context, "Code copied", android.widget.Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(
                                joinCode.uppercase(),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                        IconButton(onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(joinCode))
                            android.widget.Toast.makeText(context, "Code copied", android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    
        items(members, key = { it.id }) { member ->
            ClayCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(member.initials, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(member.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(member.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (member.role == "leader" || member.role == "admin" || member.role == "co_leader") {
                        Text(member.role.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    } else if (!isAdmin) {
                        Text(member.role.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
                    }
                    
                    if (isAdmin) {
                        var expanded by remember { mutableStateOf(false) }
                        var showRemoveConfirm by remember { mutableStateOf(false) }
                        
                        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                if (member.role != "admin" && member.role != "leader" && member.role != "co_leader") {
                                    DropdownMenuItem(
                                        text = { Text("Make Admin") },
                                        onClick = { 
                                            expanded = false
                                            onMakeAdmin(member.userId)
                                        }
                                    )
                                } else if (member.role != "leader") {
                                    DropdownMenuItem(
                                        text = { Text("Make Member") },
                                        onClick = { 
                                            expanded = false
                                            onMakeMember(member.userId)
                                        }
                                    )
                                }
                                if (member.role != "leader") {
                                    DropdownMenuItem(
                                        text = { Text("Remove from Team", color = MaterialTheme.colorScheme.error) },
                                        onClick = { 
                                            expanded = false
                                            showRemoveConfirm = true
                                        }
                                    )
                                }
                            }
                        }
                        
                        if (showRemoveConfirm) {
                            AlertDialog(
                                onDismissRequest = { showRemoveConfirm = false },
                                title = { Text("Remove Member") },
                                text = { Text("Are you sure you want to remove ${member.displayName} from this team?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showRemoveConfirm = false
                                        onRemoveMember(member.userId)
                                    }) {
                                        Text("REMOVE", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showRemoveConfirm = false }) {
                                        Text("CANCEL")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsTabContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Analytics Coming Soon", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
