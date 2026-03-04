package com.alertyai.app.ui.teams

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertyai.app.data.model.OrgMember
import com.alertyai.app.data.repository.OrgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrgMembersState(
    val members: List<OrgMember> = emptyList(),
    val joinCode: String = "",
    val isLoading: Boolean = false,
    val isLoadingCode: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OrgMembersViewModel @Inject constructor(
    private val repository: OrgRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OrgMembersState())
    val state = _state.asStateFlow()

    fun load(context: Context, orgId: String, initialJoinCode: String = "") {
        viewModelScope.launch {
            // Show the code instantly from what was already fetched by the org list
            _state.value = _state.value.copy(
                isLoading = true,
                joinCode = initialJoinCode  // no extra API call needed
            )
            // Only fetch members (not the join code — it's passed in from the org object)
            val members = repository.getOrgMembers(context, orgId)
            _state.value = _state.value.copy(members = members, isLoading = false)
        }
    }

    fun regenerateCode(context: Context, orgId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingCode = true)
            val newCode = repository.regenerateJoinCode(context, orgId)
            _state.value = _state.value.copy(
                joinCode = newCode ?: _state.value.joinCode,
                isLoadingCode = false
            )
        }
    }

    fun removeMember(context: Context, orgId: String, userId: String) {
        viewModelScope.launch {
            val success = repository.removeMember(context, orgId, userId)
            if (success) {
                _state.value = _state.value.copy(
                    members = _state.value.members.filter { it.userId != userId },
                    error = null
                )
            } else {
                _state.value = _state.value.copy(error = "Failed to remove member. Try again.")
            }
        }
    }
}
