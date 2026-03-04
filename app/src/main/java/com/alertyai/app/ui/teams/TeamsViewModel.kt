package com.alertyai.app.ui.teams

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertyai.app.data.model.Organization
import com.alertyai.app.data.model.Team
import com.alertyai.app.data.repository.OrgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamsState(
    val organizations: List<Organization> = emptyList(),
    val teams: List<Team> = emptyList(),
    val selectedOrg: Organization? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TeamsViewModel @Inject constructor(
    private val repository: OrgRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeamsState())
    val state = _state.asStateFlow()

    fun loadOrganizations(context: Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val orgs = repository.getMyOrganizations(context)
            _state.value = _state.value.copy(organizations = orgs, isLoading = false)
        }
    }

    fun createOrganization(context: Context, name: String, description: String = "") {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val joinCode = repository.createOrganization(context, name, description)
            if (joinCode != null) {
                val orgs = repository.getMyOrganizations(context)
                _state.value = _state.value.copy(organizations = orgs, isLoading = false)
            } else {
                _state.value = _state.value.copy(isLoading = false, error = "Failed to create organization")
            }
        }
    }

    fun joinByCode(context: Context, code: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = repository.joinByCode(context, code)
            if (result.isSuccess) {
                val orgs = repository.getMyOrganizations(context)
                _state.value = _state.value.copy(organizations = orgs, isLoading = false, error = null)
                onResult(true)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Invalid code"
                _state.value = _state.value.copy(isLoading = false, error = errorMsg)
                onResult(false)
            }
        }
    }

    fun selectOrganization(context: Context, org: Organization) {
        viewModelScope.launch {
            _state.value = _state.value.copy(selectedOrg = org, isLoading = true, teams = emptyList())
            val teams = repository.getOrgTeams(context, org.id)
            _state.value = _state.value.copy(teams = teams, isLoading = false)
        }
    }

    fun createTeam(context: Context, name: String, description: String = "") {
        val org = _state.value.selectedOrg ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val success = repository.createTeam(context, org.id, name, description)
            if (success) {
                val teams = repository.getOrgTeams(context, org.id)
                _state.value = _state.value.copy(teams = teams, isLoading = false, error = null)
            } else {
                _state.value = _state.value.copy(isLoading = false, error = "Failed to create team")
            }
        }
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectedOrg = null, teams = emptyList())
    }
}
