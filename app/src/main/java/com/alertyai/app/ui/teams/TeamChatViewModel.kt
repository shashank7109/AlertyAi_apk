package com.alertyai.app.ui.teams

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertyai.app.data.model.TeamChatMessage
import com.alertyai.app.data.repository.OrgRepository
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
    val isMentionListVisible: Boolean = false
)

@HiltViewModel
class TeamChatViewModel @Inject constructor(
    private val repository: OrgRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeamChatState())
    val state = _state.asStateFlow()

    private val wsManager = WebSocketManager()

    fun initChat(context: Context, orgId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            // 1. Fetch History
            val history = repository.getChatHistory(context, orgId)
            _state.value = _state.value.copy(messages = history, isLoading = false)
            
            // 2. Fetch Members for Mentions
            val members = repository.getMentionSuggestions(context, orgId)
            _state.value = _state.value.copy(mentionMembers = members)
            
            // 3. Connect WebSocket
            val token = TokenManager.getToken(context) ?: return@launch
            wsManager.connect(orgId, token, onMessage = { msg ->
                _state.value = _state.value.copy(messages = _state.value.messages + msg)
            }, onError = { t ->
                _state.value = _state.value.copy(error = t.message)
            })
        }
    }

    fun onTextChanged(text: String, selectionIndex: Int) {
        // Simple @mention detection: find if cursor is right after "@" or "@abc"
        if (selectionIndex == 0) {
            _state.value = _state.value.copy(isMentionListVisible = false)
            return
        }

        val beforeCursor = text.substring(0, selectionIndex)
        val lastAt = beforeCursor.lastIndexOf('@')
        
        if (lastAt != -1) {
            val query = beforeCursor.substring(lastAt + 1)
            // Only show suggestions if there's no space between @ and cursor
            if (!query.contains(" ")) {
                val filtered = _state.value.mentionMembers.filter {
                    val uname = it.username
                    val dname = it.displayName
                    uname.contains(query, ignoreCase = true) || 
                    dname.contains(query, ignoreCase = true)
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

    fun sendMessage(text: String) {
        if (text.isNotBlank()) {
            wsManager.sendMessage(text)
        }
    }

    override fun onCleared() {
        super.onCleared()
        wsManager.disconnect()
    }
}
