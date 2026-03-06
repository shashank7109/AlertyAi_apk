package com.alertyai.app.ui.teams.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alertyai.app.data.model.TeamChatMessage
import com.alertyai.app.network.MentionMember
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val date = try { 
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.parse(msg.timestamp) 
    } catch (e: Exception) { Date() }

    // Detect if message is a system assignment notice
    val isSystemMsg = msg.text.startsWith("\uD83D\uDCCB TASK ASSIGNED:")

    // Telegram chat bubble physics
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp, 
        topEnd = 16.dp,
        bottomStart = if (isMe) 16.dp else 2.dp,
        bottomEnd = if (isMe) 2.dp else 16.dp
    )

    // Telegram Colors
    val bubbleColor = when {
        isSystemMsg -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
        isMe -> Color(0xFFEFFDDE) 
        else -> MaterialTheme.colorScheme.surface
    }
    
    // Support dark mode bubble text appropriately
    val isDark = isSystemInDarkTheme()
    val finalBgColor = if (isMe) {
        if (isDark) Color(0xFF2B5278) else Color(0xFFE3FFC9) 
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isMe) {
        if (isDark) Color.White else Color.Black 
    } else {
        MaterialTheme.colorScheme.onSurface 
    }

    val timeColor = if (isMe) {
        if (isDark) Color(0xFFA1C9F2) else Color(0xFF45A250)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    // Parse logic for 'Reply to'
    var isReply = false
    var replyName = ""
    var replyQuote = ""
    var actualMessage = msg.text

    if (msg.text.startsWith("[Reply to ") && msg.text.contains("]: \"")) {
        try {
            val nameEndIdx = msg.text.indexOf("]: \"")
            replyName = msg.text.substring(10, nameEndIdx)
            val quoteEndIdx = msg.text.indexOf("\"\n", nameEndIdx)
            if (quoteEndIdx != -1) {
                isReply = true
                replyQuote = msg.text.substring(nameEndIdx + 4, quoteEndIdx).trim()
                actualMessage = msg.text.substring(quoteEndIdx + 2).trim()
            }
        } catch (e: Exception) {}
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = finalBgColor,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .clickable { onReply(msg) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .widthIn(max = 280.dp) 
            ) {
                if (!isMe && !isSystemMsg) {
                    Text(
                        msg.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Inner Reply Box
                if (isReply) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isDark) Color(0xFF1E3C5B) else Color(0xFFC7E6A9))
                            .padding(start = 2.dp) 
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(finalBgColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(replyName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            Text(replyQuote, style = MaterialTheme.typography.bodySmall, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                val annotatedText = buildAnnotatedString {
                    val words = actualMessage.split(" ")
                    words.forEachIndexed { index, word ->
                        if (word.startsWith("@") && word.length > 1) {
                            val mentionColor = MaterialTheme.colorScheme.primary
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Medium, color = mentionColor)) {
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
                    color = if (isSystemMsg) MaterialTheme.colorScheme.tertiary else textColor,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Inline Timestamp Telegram style 
                Text(
                    text = timeFmt.format(date ?: Date()),
                    style = MaterialTheme.typography.labelSmall, 
                    fontSize = 11.sp,
                    color = timeColor,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
            }
        }
    }
}
