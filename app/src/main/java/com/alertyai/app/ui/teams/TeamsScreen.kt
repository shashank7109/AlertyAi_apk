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
import com.alertyai.app.data.model.Organization
import com.alertyai.app.data.model.Team
import com.alertyai.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScreen(
    onTeamClick: (orgId: String, teamId: String, teamName: String) -> Unit,
    onMembersClick: (orgId: String, orgName: String, isAdmin: Boolean, joinCode: String) -> Unit
) {
    val context = LocalContext.current
    val vm: TeamsViewModel = hiltViewModel()
    val state by vm.state.collectAsState()

    var showCreateOrgDialog by remember { mutableStateOf(false) }
    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var showJoinByCodeDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.loadOrganizations(context) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                if (state.selectedOrg == null) "COMMAND CENTER" else "ORGANIZATION UNIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (state.selectedOrg == null) "HIERARCHY" else state.selectedOrg!!.name.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                        }
                    },
                    navigationIcon = {
                        if (state.selectedOrg != null) {
                            IconButton(onClick = { vm.clearSelection() }) {
                                Icon(Icons.Default.ArrowBack, "Back")
                            }
                        }
                    },
                    actions = {
                        if (state.selectedOrg == null) {
                            IconButton(onClick = { showJoinByCodeDialog = true }) {
                                Icon(Icons.Default.VpnKey, "Join by Code", tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            IconButton(onClick = {
                                state.selectedOrg?.let { org ->
                                    onMembersClick(org.id, org.name, org.isAdmin, org.joinCode)
                                }
                            }) {
                                Icon(Icons.Default.People, "View Members", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        floatingActionButton = {
            if (state.selectedOrg == null) {
                ClayButton(
                    onClick = { showCreateOrgDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("NEW ORGANIZATION", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                }
            } else if (state.selectedOrg?.isAdmin == true) {
                ClayButton(
                    onClick = { showCreateTeamDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.GroupAdd, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("NEW TEAM", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (state.organizations.isEmpty() && state.selectedOrg == null) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        modifier = Modifier.size(120.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Business, null, modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("NO HIERARCHY DETECTED", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text("Establish a new organization or deploy an access code to join an existing network.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                }
            } else if (state.selectedOrg == null) {
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Text("AVAILABLE ORGANIZATIONS", style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
                    }
                    items(state.organizations) { org ->
                        OrgCard(org, onClick = { vm.selectOrganization(context, org) })
                    }
                }
            } else {
                if (state.teams.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
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
                        Text("NO TEAMS ACTIVE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (state.selectedOrg?.isAdmin == true) "Initialize a new team unit to begin collaboration."
                            else "Contact hierarchy administrator for team assignment.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            Text("ACTIVE UNITS", style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
                        }
                        items(state.teams) { team ->
                            TeamCard(team) {
                                state.selectedOrg?.id?.let { orgId ->
                                    onTeamClick(orgId, team.id, team.name)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs remain standard but could be styled internally if needed. 
    // For now, I'll keep them consistent with the system.

    // ── Join by Code Dialog ───────────────────────────────────────────────────
    if (showJoinByCodeDialog) {
        AlertDialog(
            onDismissRequest = { showJoinByCodeDialog = false; codeInput = "" },
            title = { 
                Text("JOIN ORGANIZATION", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Input the 8-character access token provided by the hierarchy administration.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it.uppercase().take(8) },
                        placeholder = { Text("TOKEN-8") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.VpnKey, null) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black)
                    )
                    state.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                ClayButton(
                    onClick = {
                        if (codeInput.length == 8) {
                            vm.joinByCode(context, codeInput) { success ->
                                if (success) { showJoinByCodeDialog = false; codeInput = "" }
                            }
                        }
                    },
                    enabled = codeInput.length == 8 && !state.isLoading,
                    containerColor = MaterialTheme.colorScheme.primary
                ) { 
                    if (state.isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("JOIN UNIT", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall) 
                }
            },
            dismissButton = { TextButton(onClick = { showJoinByCodeDialog = false; codeInput = "" }) { Text("CANCEL") } }
        )
    }

    // ── Create Org Dialog ─────────────────────────────────────────────────────
    if (showCreateOrgDialog) {
        AlertDialog(
            onDismissRequest = { showCreateOrgDialog = false },
            title = { Text("ESTABLISH ORGANIZATION", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(value = nameInput, onValueChange = { nameInput = it },
                        placeholder = { Text("ORGANIZATION NAME") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    TextField(value = descInput, onValueChange = { descInput = it },
                        placeholder = { Text("DESC (OPTIONAL)") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                }
            },
            confirmButton = {
                ClayButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            vm.createOrganization(context, nameInput.trim(), descInput.trim())
                            showCreateOrgDialog = false; nameInput = ""; descInput = ""
                        }
                    },
                    enabled = nameInput.isNotBlank(),
                    containerColor = MaterialTheme.colorScheme.primary
                ) { Text("INITIALIZE", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall) }
            },
            dismissButton = { TextButton(onClick = { showCreateOrgDialog = false }) { Text("CANCEL") } }
        )
    }

    // ── Create Team Dialog ────────────────────────────────────────────────────
    if (showCreateTeamDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTeamDialog = false },
            title = { Text("CREATE TEAM UNIT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(value = nameInput, onValueChange = { nameInput = it },
                        placeholder = { Text("TEAM NAME") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    TextField(value = descInput, onValueChange = { descInput = it },
                        placeholder = { Text("DESC (OPTIONAL)") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                }
            },
            confirmButton = {
                ClayButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            vm.createTeam(context, nameInput.trim(), descInput.trim())
                            showCreateTeamDialog = false; nameInput = ""; descInput = ""
                        }
                    },
                    enabled = nameInput.isNotBlank(),
                    containerColor = MaterialTheme.colorScheme.primary
                ) { Text("DEPLOY", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall) }
            },
            dismissButton = { TextButton(onClick = { showCreateTeamDialog = false }) { Text("CANCEL") } }
        )
    }
}

@Composable
fun OrgCard(org: Organization, onClick: () -> Unit) {
    ClayCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(org.name.take(1).uppercase(), 
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Black, 
                        fontSize = 24.sp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(org.name.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                org.description?.let {
                    if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            // Role badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (org.isAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = if (org.isAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
            ) {
                Text(org.myRole.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (org.isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun TeamCard(team: Team, onClick: () -> Unit) {
    ClayCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                Modifier.size(44.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(team.name.uppercase(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                team.description?.let {
                    if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        }
    }
}
