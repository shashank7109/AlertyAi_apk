package com.alertyai.app.ui.teams

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val showTasksPanel: Boolean = false
)

@HiltViewModel
class TeamChatViewModel @Inject constructor(
    private val repository: OrgRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeamChatState())
    val state = _state.asStateFlow()

    private val wsManager = WebSocketManager()
    private var currentOrgId: String = ""
    private var currentContext: Context? = null

    fun initChat(context: Context, orgId: String) {
        currentOrgId = orgId
        currentContext = context
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // 1. Fetch History
            val history = repository.getChatHistory(context, orgId)
            _state.value = _state.value.copy(messages = history, isLoading = false)

            // 2. Fetch Members for Mentions and detect admin role
            val members = repository.getMentionSuggestions(context, orgId)
            _state.value = _state.value.copy(mentionMembers = members)

            // 3. Detect admin role via org list
            val orgs = repository.getMyOrganizations(context)
            val isAdmin = orgs.find { it.id == orgId }?.isAdmin ?: false
            _state.value = _state.value.copy(isAdmin = isAdmin)

            // 4. Connect WebSocket
            val token = TokenManager.getToken(context) ?: return@launch
            wsManager.connect(orgId, token, onMessage = { msg ->
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

    fun showAssignTask(member: com.alertyai.app.network.MentionMember) {
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

    fun assignTask(context: Context, title: String, description: String = "", priority: String = "normal") {
        val target = _state.value.assignTaskTarget ?: return
        viewModelScope.launch {
            val result = repository.assignTask(
                context, currentOrgId,
                AssignTaskRequest(
                    title = title,
                    description = description,
                    priority = priority,
                    assigneeEmail = target.userId // We'll use displayName as email hint; backend handles lookup
                )
            )
            result.fold(
                onSuccess = { msg ->
                    _state.value = _state.value.copy(assignTaskSuccess = msg, assignTaskError = null)
                    // Send a system notification to the chat
                    wsManager.sendMessage("📋 TASK ASSIGNED: \"$title\" → @${target.username}")
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(assignTaskError = e.message, assignTaskSuccess = null)
                }
            )
        }
    }

    fun loadTaskPanel(context: Context) {
        viewModelScope.launch {
            val isAdmin = _state.value.isAdmin
            if (isAdmin) {
                val byMe = repository.getTasksAssignedByMe(context, currentOrgId)
                _state.value = _state.value.copy(tasksAssignedByMe = byMe, showTasksPanel = true)
            } else {
                val myTasks = repository.getMyAssignedTasks(context, currentOrgId)
                _state.value = _state.value.copy(myAssignedTasks = myTasks, showTasksPanel = true)
            }
        }
    }

    fun dismissTaskPanel() {
        _state.value = _state.value.copy(showTasksPanel = false)
    }

    override fun onCleared() {
        super.onCleared()
        wsManager.disconnect()
    }
}
