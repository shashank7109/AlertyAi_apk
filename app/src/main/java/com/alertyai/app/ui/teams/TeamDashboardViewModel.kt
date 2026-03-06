package com.alertyai.app.ui.teams

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertyai.app.data.model.TeamDetailedResponse
import com.alertyai.app.data.repository.OrgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamDashboardState(
    val teamDetails: TeamDetailedResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null
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
}
