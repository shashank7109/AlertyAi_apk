package com.alertyai.app.ui.teams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
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

    LaunchedEffect(orgId) {
        vm.initChat(context, orgId)
    }

    // Auto-scroll to bottom
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "SYNC CHANNEL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            teamName.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
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
                    TeamMessageBubble(msg, isMe = msg.senderEmail == myEmail)
                }
            }

            if (state.error != null) {
                Text(
                    "CONNECTION ERROR: ${state.error}", 
                    color = MaterialTheme.colorScheme.error, 
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Black
                )
            }

            // Mention Suggestions redesign
            if (state.isMentionListVisible) {
                ClayCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .heightIn(max = 200.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    LazyColumn {
                        items(state.filteredSuggestions) { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                            }
                        }
                    }
                }
            }

            // Chat Input redesign
            ClayCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
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
                            focusedIndicatorColor = Color.Transparent
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
                        modifier = Modifier
                            .padding(4.dp)
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TeamMessageBubble(msg: TeamChatMessage, isMe: Boolean) {
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    // Parse ISO timestamp string
    val date = try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(msg.timestamp) } catch (e: Exception) { Date() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(
                msg.senderName.uppercase(), 
                style = MaterialTheme.typography.labelSmall, 
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.primary, 
                fontWeight = FontWeight.Black
            )
        }
        
        ClayCard(
            shape = RoundedCornerShape(
                topStart = 20.dp, topEnd = 20.dp,
                bottomStart = if (isMe) 20.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 20.dp
            ),
            containerColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            elevation = if (isMe) 0.dp else 4.dp
        ) {
            val annotatedText = buildAnnotatedString {
                val words = msg.text.split(" ")
                words.forEachIndexed { index, word ->
                    if (word.startsWith("@") && word.length > 1) {
                        val mentionColor = if (isMe) Color.White else MaterialTheme.colorScheme.primary
                        withStyle(style = SpanStyle(
                            fontWeight = FontWeight.Black,
                            color = mentionColor
                        )) {
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
                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Text(
            timeFmt.format(date ?: Date()), 
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 12.dp),
            fontWeight = FontWeight.Bold
        )
    }
}
