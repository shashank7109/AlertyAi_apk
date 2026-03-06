package com.alertyai.app.ui.teams

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alertyai.app.data.model.TeamChatMessage
import com.alertyai.app.network.MentionMember
import com.alertyai.app.network.TeamTask
import com.alertyai.app.network.TokenManager
import com.alertyai.app.ui.components.*
import com.alertyai.app.ui.teams.components.AssignTaskDialog
import com.alertyai.app.ui.teams.components.TaskPanelDialog
import com.alertyai.app.ui.teams.components.TeamMessageBubble
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamChatScreen(
    teamId: String,
    teamName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: TeamChatViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    var input by remember { mutableStateOf(TextFieldValue("")) }
    val myEmail = remember { TokenManager.getUserEmail(context) }

    LaunchedEffect(teamId) { vm.initChat(context, teamId) }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    // ── Assign Task Dialog ─────────────────────────────────────────────────────
    if (state.showAssignTaskDialog) {
        AssignTaskDialog(
            members = state.mentionMembers,
            success = state.assignTaskSuccess,
            error = state.assignTaskError,
            onAssign = { targets, title, desc, priority, deadline, freq, rTime -> 
                vm.assignMultipleTasks(context, targets, title, desc, priority, deadline, freq, rTime) 
            },
            onDismiss = { vm.dismissAssignTask() }
        )
    }

    // ── Task Panel (slide-up) ─────────────────────────────────────────────────
    if (state.showTasksPanel) {
        TaskPanelDialog(
            isAdmin = state.isAdmin,
            myTasks = state.myAssignedTasks,
            assignedByMeTasks = state.tasksAssignedByMe,
            onDismiss = { vm.dismissTaskPanel() }
        )
    }

    // ── Join Code Dialog ──────────────────────────────────────────────────────
    if (state.showJoinCodeDialog) {
        AlertDialog(
            onDismissRequest = { vm.dismissJoinCodeDialog() },
            title = { Text("Team Join Code", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (state.isFetchingCode) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    } else if (state.joinCode != null) {
                        Text(
                            text = state.joinCode!!,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        Text(
                            "Share this code with others so they can join ${teamName}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("Failed to load join code.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.dismissJoinCodeDialog() }) {
                    Text("DONE")
                }
            },
            dismissButton = {
                if (state.isAdmin && !state.isFetchingCode) {
                    TextButton(onClick = { vm.regenerateJoinCode(context) }) {
                        Text("REGENERATE")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SYNC CHANNEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(teamName.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    if (state.isAdmin) {
                        // Assign Task
                        IconButton(onClick = { vm.showAssignTask(null) }) {
                            Icon(Icons.Default.AddTask, "Assign Task", tint = MaterialTheme.colorScheme.primary)
                        }
                        // Join Code
                        IconButton(onClick = { vm.showJoinCodeDialog() }) {
                            Icon(Icons.Default.QrCode, "Join Code", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    // View Tasks
                    IconButton(onClick = { vm.loadTaskPanel(context) }) {
                        Icon(Icons.Default.Assignment, "Tasks", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (state.isAdmin) {
                FloatingActionButton(
                    onClick = { vm.showAssignTask(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.AddTask, "Assign Task")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                items(state.messages) { msg ->
                    TeamMessageBubble(
                        msg = msg,
                        isMe = msg.senderEmail == myEmail,
                        isAdmin = state.isAdmin,
                        members = state.mentionMembers,
                        onReply = { vm.setReplyingTo(it) },
                        onAssignTask = { member -> vm.showAssignTask(member) }
                    )
                }
            }

            if (state.error != null) {
                Text("CONNECTION ERROR: ${state.error}", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Medium)
            }

            // Mention Suggestions
            if (state.isMentionListVisible) {
                ClayCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).heightIn(max = 200.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    LazyColumn {
                        items(state.filteredSuggestions) { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        val textBefore = input.text.substring(0, input.selection.start)
                                        val lastAt = textBefore.lastIndexOf('@')
                                        val newText = textBefore.substring(0, lastAt + 1) + member.username + " " + input.text.substring(input.selection.end)
                                        val newCursor = lastAt + 1 + member.username.length + 1
                                        input = TextFieldValue(newText, selection = androidx.compose.ui.text.TextRange(newCursor))
                                        vm.dismissMentionList()
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("@", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                    }
                                }
                                Column {
                                    Text(member.username, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(member.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Reply Preview
            AnimatedVisibility(visible = state.replyingTo != null) {
                state.replyingTo?.let { replyMsg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("REPLYING TO ${replyMsg.senderName.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(replyMsg.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { vm.setReplyingTo(null) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Chat Input
            ClayCard(modifier = Modifier.fillMaxWidth().padding(20.dp), shape = RoundedCornerShape(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = input,
                        onValueChange = {
                            input = it
                            vm.onTextChanged(it.text, it.selection.start)
                        },
                        placeholder = { Text("TRANSMIT MESSAGE...") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (input.text.isNotBlank()) {
                                vm.sendMessage(input.text)
                                input = TextFieldValue("")
                                keyboard?.hide()
                            }
                        })
                    )
                    if (state.isAdmin) {
                        IconButton(
                            onClick = { vm.showAssignTask(null) },
                            modifier = Modifier.padding(4.dp).size(40.dp).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Assignment, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(
                        onClick = {
                            if (input.text.isNotBlank()) {
                                vm.sendMessage(input.text)
                                input = TextFieldValue("")
                                keyboard?.hide()
                            }
                        },
                        modifier = Modifier.padding(4.dp).size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}


