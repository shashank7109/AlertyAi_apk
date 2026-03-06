package com.alertyai.app.ui.teams

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertyai.app.data.model.PendingInvitation
import com.alertyai.app.data.model.TeamDetailedResponse
import com.alertyai.app.data.repository.OrgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamsState(
    val teams: List<TeamDetailedResponse> = emptyList(),
    val invitations: List<PendingInvitation> = emptyList(),
    val isLoading: Boolean = false,
    val isProcessingInvite: String? = null,
    val error: String? = null
)

@HiltViewModel
class TeamsViewModel @Inject constructor(
    private val repository: OrgRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeamsState())
    val state = _state.asStateFlow()

    fun loadData(context: Context) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                android.util.Log.d("TeamsViewModel", "Loading teams...")
                val teamsList = repository.getTeams(context)
                android.util.Log.d("TeamsViewModel", "Fetched ${teamsList.size} teams")
                
                val invsList = repository.getPendingInvitations(context)
                android.util.Log.d("TeamsViewModel", "Fetched ${invsList.size} invitations")
                
                _state.value = _state.value.copy(
                    teams = teamsList, 
                    invitations = invsList, 
                    isLoading = false
                )
            } catch (e: Exception) {
                android.util.Log.e("TeamsViewModel", "Error loading data", e)
                _state.value = _state.value.copy(
                    isLoading = false, 
                    error = "Failed to load teams: ${e.message}"
                )
            }
        }
    }

    fun createTeam(
        context: Context, 
        name: String, 
        description: String = "",
        purpose: String = "other",
        memberEmails: List<String> = emptyList(),
        memberPhones: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val success = repository.createTeam(context, name, description, purpose, memberEmails, memberPhones)
            if (success) {
                loadData(context)
            } else {
                _state.value = _state.value.copy(isLoading = false, error = "Failed to create team")
            }
        }
    }

    fun acceptInvitation(context: Context, invitationId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessingInvite = invitationId)
            val success = repository.acceptInvitation(context, invitationId)
            if (success) {
                loadData(context)
            } else {
                _state.value = _state.value.copy(error = "Failed to accept invitation")
            }
            _state.value = _state.value.copy(isProcessingInvite = null)
        }
    }

    fun declineInvitation(context: Context, invitationId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessingInvite = invitationId)
            val success = repository.declineInvitation(context, invitationId)
            if (success) {
                loadData(context)
            } else {
                _state.value = _state.value.copy(error = "Failed to decline invitation")
            }
            _state.value = _state.value.copy(isProcessingInvite = null)
        }
    }

    fun joinTeamByCode(context: Context, code: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.joinTeamByCode(context, code)
            if (result.isSuccess) {
                loadData(context)
                onSuccess()
            } else {
                _state.value = _state.value.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Failed to join team by code")
            }
        }
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
