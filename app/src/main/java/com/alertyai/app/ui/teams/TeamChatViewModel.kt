package com.alertyai.app.ui.teams

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertyai.app.MainActivity
import com.alertyai.app.data.model.TeamChatMessage
import com.alertyai.app.data.repository.OrgRepository
import com.alertyai.app.network.AssignTaskRequest
import com.alertyai.app.network.TeamTask
import com.alertyai.app.network.TokenManager
import com.alertyai.app.network.WebSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamChatState(
    val messages: List<TeamChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val mentionMembers: List<com.alertyai.app.network.MentionMember> = emptyList(),
    val filteredSuggestions: List<com.alertyai.app.network.MentionMember> = emptyList(),
    val isMentionListVisible: Boolean = false,
    // Reply feature
    val replyingTo: TeamChatMessage? = null,
    // Admin task assignment
    val isAdmin: Boolean = false,
    val showAssignTaskDialog: Boolean = false,
    val assignTaskTarget: com.alertyai.app.network.MentionMember? = null,
    val assignTaskSuccess: String? = null,
    val assignTaskError: String? = null,
    // Task lists
    val myAssignedTasks: List<TeamTask> = emptyList(),
    val tasksAssignedByMe: List<TeamTask> = emptyList(),
    val showTasksPanel: Boolean = false,
    
    // Join code
    val joinCode: String? = null,
    val isFetchingCode: Boolean = false,
    val showJoinCodeDialog: Boolean = false
)

@HiltViewModel
class TeamChatViewModel @Inject constructor(
    private val repository: OrgRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeamChatState())
    val state = _state.asStateFlow()

    private val wsManager = WebSocketManager()
    private var currentTeamId: String = ""
    private var currentContext: Context? = null

    fun initChat(context: Context, teamId: String) {
        currentTeamId = teamId
        currentContext = context
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // 1. Fetch History
            val history = repository.getChatHistory(context, teamId)
            _state.value = _state.value.copy(messages = history, isLoading = false)

            // 2. Fetch Members for Mentions and detect admin role
            val members = repository.getMentionSuggestions(context, teamId)
            _state.value = _state.value.copy(mentionMembers = members)

            // 3. Detect admin role via team list
            val orgs = repository.getMyOrganizations(context) // TODO: Rename getMyOrganizations to Teams later if needed
            val isAdmin = orgs.find { it.id == teamId }?.isAdmin ?: false
            _state.value = _state.value.copy(isAdmin = isAdmin)

            // 4. Connect WebSocket
            val token = TokenManager.getToken(context) ?: return@launch
            wsManager.connect(teamId, token, onMessage = { msg ->
                val myEmail = TokenManager.getUserEmail(context)
                if (msg.senderEmail != myEmail && myEmail.isNotEmpty()) {
                    showNotification(context, msg)
                }
                _state.value = _state.value.copy(messages = _state.value.messages + msg)
            }, onError = { t ->
                _state.value = _state.value.copy(error = t.message)
            })
        }
    }

    fun onTextChanged(text: String, selectionIndex: Int) {
        if (selectionIndex == 0) {
            _state.value = _state.value.copy(isMentionListVisible = false)
            return
        }
        val beforeCursor = text.substring(0, selectionIndex)
        val lastAt = beforeCursor.lastIndexOf('@')
        if (lastAt != -1) {
            val query = beforeCursor.substring(lastAt + 1)
            if (!query.contains(" ")) {
                val filtered = _state.value.mentionMembers.filter {
                    it.username.contains(query, ignoreCase = true) ||
                    it.displayName.contains(query, ignoreCase = true)
                }
                _state.value = _state.value.copy(
                    filteredSuggestions = filtered,
                    isMentionListVisible = filtered.isNotEmpty()
                )
                return
            }
        }
        _state.value = _state.value.copy(isMentionListVisible = false)
    }

    fun dismissMentionList() {
        _state.value = _state.value.copy(isMentionListVisible = false)
    }

    // ── Reply Feature ──────────────────────────────────────────────────────────

    fun setReplyingTo(msg: TeamChatMessage?) {
        _state.value = _state.value.copy(replyingTo = msg)
    }

    fun sendMessage(text: String) {
        if (text.isNotBlank()) {
            val replyTo = _state.value.replyingTo
            val finalText = if (replyTo != null) {
                "[Reply to ${replyTo.senderName}]: \"${replyTo.text.take(60)}...\"\n$text"
            } else text
            wsManager.sendMessage(finalText)
            _state.value = _state.value.copy(replyingTo = null)
        }
    }

    // ── Admin Task Assignment ──────────────────────────────────────────────────

    fun showAssignTask(member: com.alertyai.app.network.MentionMember?) {
        _state.value = _state.value.copy(showAssignTaskDialog = true, assignTaskTarget = member)
    }

    fun dismissAssignTask() {
        _state.value = _state.value.copy(
            showAssignTaskDialog = false,
            assignTaskTarget = null,
            assignTaskSuccess = null,
            assignTaskError = null
        )
    }

    fun assignMultipleTasks(
        context: Context,
        targets: List<com.alertyai.app.network.MentionMember>,
        title: String,
        description: String = "",
        priority: String = "medium",
        deadline: String? = null,
        reminderFrequency: String = "daily",
        reminderTime: String? = null
    ) {
        viewModelScope.launch {
            if (targets.isEmpty()) return@launch
            var successCount = 0
            
            for (target in targets) {
                val req = AssignTaskRequest(
                    title = title,
                    description = description,
                    priority = priority,
                    deadline = deadline,
                    reminderFrequency = reminderFrequency,
                    reminderTime = reminderTime,
                    assignedTo = target.userId // backend handles lookup
                )
                val result = repository.assignTask(context, currentTeamId, req)

                if (result.isSuccess) {
                    successCount++
                }
            }

            if (successCount > 0) {
                _state.value = _state.value.copy(
                    assignTaskSuccess = "Successfully assigned task to $successCount members.",
                    assignTaskError = null
                )
                val assignedTo = targets.joinToString(", ") { "@" + it.username }
                wsManager.sendMessage("📋 TASK ASSIGNED: \"$title\" → $assignedTo")
            } else {
                _state.value = _state.value.copy(
                    assignTaskError = "Failed to assign tasks",
                    assignTaskSuccess = null
                )
            }
        }
    }

    fun loadTaskPanel(context: Context) {
        viewModelScope.launch {
            val isAdmin = _state.value.isAdmin
            if (isAdmin) {
                val byMe = repository.getTasksAssignedByMe(context, currentTeamId)
                _state.value = _state.value.copy(tasksAssignedByMe = byMe, showTasksPanel = true)
            } else {
                val myTasks = repository.getMyAssignedTasks(context, currentTeamId)
                _state.value = _state.value.copy(myAssignedTasks = myTasks, showTasksPanel = true)
            }
        }
    }

    fun dismissTaskPanel() {
        _state.value = _state.value.copy(showTasksPanel = false)
    }

    // ── Join Code ──────────────────────────────────────────────────────────────

    fun showJoinCodeDialog() {
        _state.value = _state.value.copy(showJoinCodeDialog = true)
        currentContext?.let { getJoinCode(it) }
    }

    fun dismissJoinCodeDialog() {
        _state.value = _state.value.copy(showJoinCodeDialog = false)
    }

    private fun getJoinCode(context: Context) {
         if (currentTeamId.isEmpty()) return
         viewModelScope.launch {
             _state.value = _state.value.copy(isFetchingCode = true)
             val code = repository.getJoinCode(context, currentTeamId)
             _state.value = _state.value.copy(isFetchingCode = false, joinCode = code)
         }
    }

    fun regenerateJoinCode(context: Context) {
        if (currentTeamId.isEmpty()) return
        viewModelScope.launch {
             _state.value = _state.value.copy(isFetchingCode = true)
             val code = repository.regenerateJoinCode(context, currentTeamId)
             _state.value = _state.value.copy(isFetchingCode = false, joinCode = code)
        }
    }

    override fun onCleared() {
        super.onCleared()
        wsManager.disconnect()
    }

    private fun showNotification(context: Context, msg: TeamChatMessage) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "team_chat_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Team Chat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New message from ${msg.senderName}")
            .setContentText(msg.text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(msg.id.hashCode(), notification)
    }
}
