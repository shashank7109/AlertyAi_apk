package com.alertyai.app.ui.teams

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alertyai.app.data.model.PendingInvitation
import com.alertyai.app.data.model.TeamDetailedResponse
import com.alertyai.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScreen(
    onTeamClick: (teamId: String, teamName: String) -> Unit
) {
    val context = LocalContext.current
    val vm: TeamsViewModel = hiltViewModel()
    val state by vm.state.collectAsState()

    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var showJoinTeamDialog by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var purposeInput by remember { mutableStateOf("other") }
    var inviteInput by remember { mutableStateOf("") }

    val purposes = listOf(
        "hackathon" to "💻 Hackathon",
        "college_project" to "🎓 College Project",
        "office_work" to "💼 Office Work",
        "startup" to "🚀 Startup",
        "freelance" to "💰 Freelance",
        "family" to "👨‍👩‍👧‍👦 Family",
        "ngo" to "🤝 NGO",
        "farmers_group" to "🌾 Farmers",
        "shop_staff" to "🏪 Shop Staff",
        "construction" to "🏗️ Construction",
        "event_management" to "🎉 Events",
        "other" to "📋 Other"
    )

    LaunchedEffect(Unit) { vm.loadData(context) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "My Teams",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Collaborate with your team members on tasks and projects",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        IconButton(onClick = { showJoinTeamDialog = true }) {
                            Icon(Icons.Default.AddLink, contentDescription = "Join Team via Code")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            ClayButton(
                onClick = { showCreateTeamDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("CREATE TEAM", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelSmall)
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    if (state.invitations.isNotEmpty()) {
                        item {
                            Text("Pending Invitations", style = MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        }
                        items(state.invitations) { inv ->
                            InvitationCard(
                                invitation = inv,
                                isProcessing = state.isProcessingInvite == inv.id,
                                onAccept = { vm.acceptInvitation(context, inv.id) },
                                onDecline = { vm.declineInvitation(context, inv.id) }
                            )
                        }
                    }

                    if (state.teams.isEmpty()) {
                        item {
                            Column(
                                Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                    modifier = Modifier.size(120.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Groups, null, modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                                Text("No Teams Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Create your first team to start collaborating with others",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        item {
                            Text("Active Teams", style = MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        }
                        items(state.teams) { team ->
                            FlatTeamCard(team) {
                                onTeamClick(team.id, team.name)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Create Team Dialog ────────────────────────────────────────────────────
    if (showCreateTeamDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTeamDialog = false },
            title = { Text("CREATE TEAM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it },
                        label = { Text("Team Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    
                    OutlinedTextField(value = descInput, onValueChange = { descInput = it },
                        label = { Text("Description (Optional)") }, maxLines = 3, modifier = Modifier.fillMaxWidth())

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = purposes.find { it.first == purposeInput }?.second ?: purposeInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Purpose") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            purposes.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { purposeInput = key; expanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inviteInput,
                        onValueChange = { inviteInput = it },
                        label = { Text("Invite Members (Emails or Phones separated by comma)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                }
            },
            confirmButton = {
                ClayButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            val items = inviteInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            val emails = items.filter { it.contains("@") }
                            val phones = items.filter { !it.contains("@") }
                            
                            vm.createTeam(
                                context, 
                                nameInput.trim(), 
                                descInput.trim(), 
                                purposeInput,
                                memberEmails = emails,
                                memberPhones = phones
                            )
                            showCreateTeamDialog = false
                            nameInput = ""
                            descInput = ""
                            purposeInput = "other"
                            inviteInput = ""
                        }
                    },
                    enabled = nameInput.isNotBlank(),
                    containerColor = MaterialTheme.colorScheme.primary
                ) { Text("Create", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelSmall) }
            },
            dismissButton = { TextButton(onClick = { showCreateTeamDialog = false }) { Text("CANCEL") } }
        )
    }
    // ── Join Team Dialog ──────────────────────────────────────────────────────
    if (showJoinTeamDialog) {
        AlertDialog(
            onDismissRequest = { showJoinTeamDialog = false },
            title = { Text("JOIN TEAM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter the 6-digit code provided by your team leader.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = joinCodeInput,
                        onValueChange = { joinCodeInput = it.uppercase() },
                        label = { Text("6-Digit Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                }
            },
            confirmButton = {
                ClayButton(
                    onClick = {
                        if (joinCodeInput.trim().length == 6) {
                            vm.joinTeamByCode(context, joinCodeInput.trim()) {
                                showJoinTeamDialog = false
                                joinCodeInput = ""
                            }
                        }
                    },
                    enabled = joinCodeInput.trim().length == 6 && !state.isLoading,
                    containerColor = MaterialTheme.colorScheme.primary
                ) { Text(if (state.isLoading) "JOINING..." else "JOIN", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelSmall) }
            },
            dismissButton = { TextButton(onClick = { showJoinTeamDialog = false; vm.clearError() }) { Text("CANCEL") } }
        )
    }
}

@Composable
fun InvitationCard(invitation: PendingInvitation, isProcessing: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(invitation.teamName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Invited by ${invitation.invitedByName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isProcessing) {
                CircularProgressIndicator(Modifier.size(24.dp))
            } else {
                IconButton(onClick = onDecline) {
                    Icon(Icons.Default.Close, "Decline", tint = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                    Text("Accept")
                }
            }
        }
    }
}

@Composable
fun FlatTeamCard(team: TeamDetailedResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            when(team.purpose) {
                                "hackathon" -> "💻"
                                "college_project" -> "🎓"
                                "office_work" -> "💼"
                                "startup" -> "🚀"
                                "freelance" -> "💰"
                                "family" -> "👨‍👩‍👧‍👦"
                                "ngo" -> "🤝"
                                "farmers_group" -> "🌾"
                                "shop_staff" -> "🏪"
                                "construction" -> "🏗️"
                                "event_management" -> "🎉"
                                else -> "📋"
                            },
                            fontSize = 24.sp
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(team.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    team.description?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val totalTasks = team.tasks?.size ?: 0
                val completedTasks = team.tasks?.count { it.status == "completed" } ?: 0
                val membersCount = team.members?.size ?: 0

                StatBadge(Icons.Default.People, "$membersCount Members")
                StatBadge(Icons.Default.CheckCircle, "$completedTasks Done", tint = Color(0xFF4CAF50))
                StatBadge(Icons.Default.List, "$totalTasks Tasks")
            }
        }
    }
}

@Composable
private fun StatBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = tint)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
