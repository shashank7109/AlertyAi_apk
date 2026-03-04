package com.alertyai.app.ui.teams

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.alertyai.app.data.model.OrgMember
import com.alertyai.app.ui.components.*
import com.alertyai.app.ui.theme.MonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgMembersScreen(
    orgId: String,
    orgName: String,
    isAdmin: Boolean,
    initialJoinCode: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: OrgMembersViewModel = hiltViewModel()
    val state by vm.state.collectAsState()

    var showRemoveDialog by remember { mutableStateOf<OrgMember?>(null) }

    LaunchedEffect(orgId) {
        vm.load(context, orgId, initialJoinCode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "HIERARCHY UNIT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            orgName.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            state.members.size.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Invite Code Card
            item {
                JoinCodeCard(
                    joinCode = state.joinCode,
                    isAdmin = isAdmin,
                    isLoadingCode = state.isLoadingCode,
                    onCopy = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Invite Code", state.joinCode))
                        Toast.makeText(context, "Access Token Copied", Toast.LENGTH_SHORT).show()
                    },
                    onRegenerate = {
                        if (isAdmin) vm.regenerateCode(context, orgId)
                    }
                )
            }

            item {
                Text("AUTHORIZED PERSONNEL", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 12.dp))
            }

            // Member List
            if (state.isLoading) {
                item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }}
            } else if (state.members.isEmpty()) {
                item { 
                    Text("NO PERSONNEL REGISTERED", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium) 
                }
            } else {
                items(state.members, key = { it.userId }) { member ->
                    MemberRow(
                        member = member,
                        canRemove = isAdmin && !member.isAdmin,
                        onRemove = { showRemoveDialog = member }
                    )
                }
            }

            state.error?.let { err ->
                item { 
                    Text(err.uppercase(), color = MaterialTheme.colorScheme.error, 
                        modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium) 
                }
            }
        }
    }

    // Remove Confirmation Dialog
    showRemoveDialog?.let { member ->
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            title = { Text("REVOKE ACCESS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium) },
            text = { Text("Terminate organizational access for ${member.displayName}? This will isolate the user from all secure channels.") },
            confirmButton = {
                ClayButton(
                    onClick = {
                        vm.removeMember(context, orgId, member.userId)
                        showRemoveDialog = null
                    },
                    containerColor = MaterialTheme.colorScheme.error
                ) { Text("REVOKE", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelSmall) }
            },
            dismissButton = { TextButton(onClick = { showRemoveDialog = null }) { Text("CANCEL") } }
        )
    }
}

@Composable
fun JoinCodeCard(
    joinCode: String,
    isAdmin: Boolean,
    isLoadingCode: Boolean,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit
) {
    ClayCard(
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ORGANIZATIONAL ACCESS TOKEN", 
                fontWeight = FontWeight.Medium, 
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
            
            Spacer(Modifier.height(16.dp))
            
            if (isLoadingCode) {
                Box(Modifier.height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = joinCode.ifBlank { "........" },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MonoFontFamily,
                        letterSpacing = 4.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text("Distribute this token to authorize new personnel for deployment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
            
            Spacer(Modifier.height(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ClayButton(onClick = onCopy, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("COPY TOKEN", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelSmall)
                }
                
                if (isAdmin) {
                    ClayButton(onClick = onRegenerate, containerColor = MaterialTheme.colorScheme.surface) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("RESET", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun MemberRow(member: OrgMember, canRemove: Boolean, onRemove: () -> Unit) {
    ClayCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                color = if (member.isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        member.initials.uppercase(),
                        color = if (member.isAdmin) Color.White else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium, fontSize = 20.sp
                    )
                }
            }
            
            Column(Modifier.weight(1f)) {
                Text(member.displayName.uppercase(), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                Text(member.email.lowercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (member.isAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = if (member.isAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                    shape = RoundedCornerShape(6.dp)
                )
            ) {
                Text(
                    member.role.uppercase(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (member.isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (canRemove) {
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.PersonRemove, null,
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
