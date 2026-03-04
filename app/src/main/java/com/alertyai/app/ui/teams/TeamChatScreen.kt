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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamChatScreen(
    orgId: String,
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

    LaunchedEffect(orgId) { vm.initChat(context, orgId) }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    // ── Assign Task Dialog ─────────────────────────────────────────────────────
    if (state.showAssignTaskDialog) {
        AssignTaskDialog(
            target = state.assignTaskTarget,
            success = state.assignTaskSuccess,
            error = state.assignTaskError,
            onAssign = { title, desc, priority -> vm.assignTask(context, title, desc, priority) },
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SYNC CHANNEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(teamName.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { vm.loadTaskPanel(context) }) {
                        Icon(Icons.Default.Task, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
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
                    modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Black)
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
                                        Text("@", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                                    }
                                }
                                Column {
                                    Text(member.username, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
                                    Text(member.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                // Admin can assign directly from mention list
                                if (state.isAdmin) {
                                    Spacer(Modifier.weight(1f))
                                    Surface(
                                        onClick = { vm.showAssignTask(member); vm.dismissMentionList() },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ) {
                                        Text("ASSIGN", fontSize = 9.sp, fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
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

// ── Message Bubble ─────────────────────────────────────────────────────────────

@Composable
fun TeamMessageBubble(
    msg: TeamChatMessage,
    isMe: Boolean,
    isAdmin: Boolean = false,
    members: List<MentionMember> = emptyList(),
    onReply: (TeamChatMessage) -> Unit,
    onAssignTask: (MentionMember) -> Unit = {}
) {
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val date = try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(msg.timestamp) } catch (e: Exception) { Date() }

    // Detect if message is a system assignment notice
    val isSystemMsg = msg.text.startsWith("📋 TASK ASSIGNED:")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(msg.senderName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                color = if (isSystemMsg) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black)
        }

        ClayCard(
            shape = RoundedCornerShape(
                topStart = 20.dp, topEnd = 20.dp,
                bottomStart = if (isMe) 20.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 20.dp
            ),
            containerColor = when {
                isSystemMsg -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                isMe -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surface
            },
            elevation = if (isMe) 0.dp else 4.dp
        ) {
            val annotatedText = buildAnnotatedString {
                val words = msg.text.split(" ")
                words.forEachIndexed { index, word ->
                    if (word.startsWith("@") && word.length > 1) {
                        val mentionColor = if (isMe) Color.White else MaterialTheme.colorScheme.primary
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Black, color = mentionColor)) {
                            append(word)
                        }
                    } else {
                        append(word)
                    }
                    if (index < words.size - 1) append(" ")
                }
            }
            Text(
                text = annotatedText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = when {
                    isSystemMsg -> MaterialTheme.colorScheme.tertiary
                    isMe -> Color.White
                    else -> MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Timestamp + REPLY + ASSIGN TASK (admin only in context of a sender's message)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 12.dp)
        ) {
            Text(timeFmt.format(date ?: Date()),
                style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)

            // Reply button for all messages
            Text("REPLY", fontSize = 9.sp, fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.clickable { onReply(msg) }.padding(4.dp))

            // Assign Task button - admin only, only for other users' messages
            if (isAdmin && !isMe && !isSystemMsg) {
                val senderMember = members.find { it.displayName == msg.senderName || it.username == msg.senderEmail }
                if (senderMember != null) {
                    Text("ASSIGN TASK", fontSize = 9.sp, fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f),
                        modifier = Modifier.clickable { onAssignTask(senderMember) }.padding(4.dp))
                }
            }
        }
    }
}

// ── Assign Task Dialog ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignTaskDialog(
    target: MentionMember?,
    success: String?,
    error: String?,
    onAssign: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("normal") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("ASSIGN TASK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                target?.let {
                    Text("→ ${it.displayName.uppercase()}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (success != null) {
                    Text(success.uppercase(), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                }
                if (error != null) {
                    Text(error, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                }
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("TASK TITLE") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("DESCRIPTION (OPTIONAL)") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3
                )
                Text("PRIORITY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black,
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
                            Text(p.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black,
                                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) onAssign(title, description, priority)
            }, enabled = title.isNotBlank()) {
                Text("DEPLOY TASK", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

// ── Task Panel Dialog ──────────────────────────────────────────────────────────

@Composable
fun TaskPanelDialog(
    isAdmin: Boolean,
    myTasks: List<TeamTask>,
    assignedByMeTasks: List<TeamTask>,
    onDismiss: () -> Unit
) {
    val tasks = if (isAdmin) assignedByMeTasks else myTasks
    val title = if (isAdmin) "TASKS I ASSIGNED" else "MY ASSIGNED TASKS"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                if (tasks.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("NO TASKS YET", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tasks) { task ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(task.title.uppercase(), style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                                        val statusColor = when (task.status) {
                                            "completed" -> Color(0xFF22C55E)
                                            "in_progress" -> Color(0xFFF97316)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                        Text(task.status.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black,
                                            color = statusColor, modifier = Modifier
                                                .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp))
                                    }
                                    if (isAdmin) {
                                        Text("→ ${task.assigneeEmail}", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("From: ${task.assignedByEmail}", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}
