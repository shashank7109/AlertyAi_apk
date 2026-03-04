package com.alertyai.app.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertyai.app.network.GoogleIdTokenRequest
import com.alertyai.app.network.LoginRequest
import com.alertyai.app.network.RetrofitClient
import com.alertyai.app.network.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val userName: String? = null
)

class AuthViewModel : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    // ── Email / Password login ─────────────────────────────────────────────────
    fun login(context: Context, email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(error = "Email and password are required")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val response = RetrofitClient.api.login(LoginRequest(email.trim(), password))
                if (response.isSuccessful) {
                    val body = response.body()
                    val token = body?.resolvedToken
                    if (!token.isNullOrBlank()) {
                        TokenManager.saveToken(context, token, email.trim())
                        _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = body?.message ?: "Login failed — no token received"
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Invalid email or password"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Cannot reach server. Is the backend running?\n${e.localizedMessage}"
                )
            }
        }
    }

    // ── Google Sign-In (Credential Manager → backend mobile-token) ─────────────
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                // Step 1: get Google ID Token via Credential Manager
                val tokenResult = GoogleSignInHelper.getIdToken(context)
                if (tokenResult.isFailure) {
                    val msg = tokenResult.exceptionOrNull()?.localizedMessage ?: "Google sign-in cancelled"
                    _state.value = _state.value.copy(isLoading = false, error = msg)
                    return@launch
                }

                val googleIdToken = tokenResult.getOrThrow()

                // Step 2: exchange Google ID Token for AlertyAI JWT
                val response = RetrofitClient.api.googleMobileSignIn(
                    GoogleIdTokenRequest(googleIdToken)
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val token = body?.resolvedToken
                    val email = body?.user?.email ?: ""
                    val name = body?.user?.fullName ?: body?.user?.name ?: ""
                    if (!token.isNullOrBlank()) {
                        TokenManager.saveToken(context, token, email)
                        _state.value = _state.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            userName = name.ifBlank { email }
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Google sign-in succeeded but no token received"
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Server rejected Google token (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Google sign-in failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
