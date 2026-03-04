package com.alertyai.app.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertyai.app.network.RetrofitClient
import com.alertyai.app.network.TokenManager
import com.alertyai.app.network.UserInfo
import com.alertyai.app.network.UserUpdateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: UserInfo? = null,
    val error: String? = null,
    val updateSuccess: Boolean = false
)

class ProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state

    fun loadProfile(context: Context) {
        val token = TokenManager.getToken(context) ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val response = RetrofitClient.api.getProfile("Bearer $token")
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, profile = response.body())
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Failed to load profile: ${response.code()}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun updateProfile(context: Context, name: String, username: String, mobile: String, profilePic: String) {
        val token = TokenManager.getToken(context) ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, updateSuccess = false)
            try {
                val request = UserUpdateRequest(
                    name = name,
                    username = username.ifBlank { null },
                    mobileNumber = mobile.ifBlank { null },
                    profilePicture = profilePic.ifBlank { null }
                )
                val response = RetrofitClient.api.updateProfile("Bearer $token", request)
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        profile = response.body(),
                        updateSuccess = true
                    )
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Update failed"
                    _state.value = _state.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun resetUpdateSuccess() {
        _state.value = _state.value.copy(updateSuccess = false)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
