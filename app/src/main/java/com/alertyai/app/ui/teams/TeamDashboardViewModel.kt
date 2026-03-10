package com.alertyai.app.ui.teams

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertyai.app.data.model.TeamDetailedResponse
import com.alertyai.app.network.AssignTaskRequest
import com.alertyai.app.data.repository.OrgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamDashboardState(
    val teamDetails: TeamDetailedResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionLoading: Boolean = false,
    val actionSuccess: String? = null,
    val actionError: String? = null
)

@HiltViewModel
class TeamDashboardViewModel @Inject constructor(
    private val repository: OrgRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeamDashboardState())
    val state = _state.asStateFlow()

    fun loadTeamDetails(context: Context, teamId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val details = repository.getTeam(context, teamId)
            if (details != null) {
                _state.value = _state.value.copy(teamDetails = details, isLoading = false)
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load team details."
                )
            }
        }
    }
    fun deleteTeam(context: Context, teamId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionLoading = true, actionError = null)
            val result = repository.deleteTeam(context, teamId)
            if (result.isSuccess) {
                _state.value = _state.value.copy(actionLoading = false, actionSuccess = "Team deleted successfully")
                onSuccess()
            } else {
                _state.value = _state.value.copy(actionLoading = false, actionError = result.exceptionOrNull()?.message ?: "Failed to delete team")
            }
        }
    }

    fun updateMemberRole(context: Context, teamId: String, userId: String, newRole: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionLoading = true, actionError = null)
            val result = repository.updateMemberRole(context, teamId, userId, newRole)
            if (result.isSuccess) {
                _state.value = _state.value.copy(actionLoading = false, actionSuccess = "Role updated successfully")
                loadTeamDetails(context, teamId) // Reload to reflect changes
            } else {
                _state.value = _state.value.copy(actionLoading = false, actionError = result.exceptionOrNull()?.message ?: "Failed to update role")
            }
        }
    }

    fun removeMember(context: Context, teamId: String, userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionLoading = true, actionError = null)
            val result = repository.removeTeamMember(context, teamId, userId)
            if (result.isSuccess) {
                _state.value = _state.value.copy(actionLoading = false, actionSuccess = "Member removed successfully")
                loadTeamDetails(context, teamId) // Reload to reflect changes
            } else {
                _state.value = _state.value.copy(actionLoading = false, actionError = result.exceptionOrNull()?.message ?: "Failed to remove member")
            }
        }
    }

    fun assignTask(context: Context, teamId: String, title: String, description: String, priority: String, assignedTo: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionLoading = true, actionError = null)
            val req = AssignTaskRequest(
                title = title,
                description = description,
                priority = priority,
                assignedTo = assignedTo
            )
            val result = repository.assignTask(context, teamId, req)
            if (result.isSuccess) {
                _state.value = _state.value.copy(actionLoading = false, actionSuccess = "Task assigned successfully")
                loadTeamDetails(context, teamId)
            } else {
                _state.value = _state.value.copy(actionLoading = false, actionError = result.exceptionOrNull()?.message ?: "Failed to assign task")
            }
        }
    }

    fun dismissActionMessage() {
        _state.value = _state.value.copy(actionSuccess = null, actionError = null)
    }
}
