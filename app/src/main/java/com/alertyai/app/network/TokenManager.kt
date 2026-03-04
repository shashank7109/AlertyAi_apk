package com.alertyai.app.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONObject

/**
 * Manages JWT token storage in SharedPreferences.
 * Includes expiry detection to proactively catch and handle token expiry before making API calls.
 */
object TokenManager {
    private const val PREF_FILE = "alertyai_prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_EMAIL = "user_email"

    // Buffer: refresh token 2 minutes before actual expiry to avoid race conditions
    private const val EXPIRY_BUFFER_MS = 2 * 60 * 1000L

    private val _isLoggedInState = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isLoggedInState: kotlinx.coroutines.flow.StateFlow<Boolean> = _isLoggedInState

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    fun saveToken(context: Context, token: String, email: String = "") {
        getPrefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .apply()
        _isLoggedInState.value = true
    }

    fun getToken(context: Context): String? = getPrefs(context).getString(KEY_TOKEN, null)

    fun getUserEmail(context: Context): String = getPrefs(context).getString(KEY_EMAIL, "") ?: ""

    fun clearToken(context: Context) {
        getPrefs(context).edit().clear().apply()
        _isLoggedInState.value = false
    }

    fun isLoggedIn(context: Context): Boolean {
        val loggedIn = !getToken(context).isNullOrBlank()
        _isLoggedInState.value = loggedIn
        return loggedIn
    }

    /**
     * Returns true if the stored JWT has expired (or is about to expire within 2 minutes).
     * Decodes the JWT payload without verifying signature — used for proactive expiry checks only.
     */
    fun isTokenExpired(context: Context): Boolean {
        val token = getToken(context) ?: return true
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING), Charsets.UTF_8)
            val json = JSONObject(payload)
            val exp = json.optLong("exp", 0L)
            if (exp == 0L) return false // No expiry field → treat as valid
            val expMs = exp * 1000L
            val nowMs = System.currentTimeMillis()
            (expMs - nowMs) < EXPIRY_BUFFER_MS
        } catch (e: Exception) {
            false // On any parsing error, assume valid and let server reject
        }
    }

    /**
     * Get token if valid, or return null if expired.
     * Callers should trigger re-auth when this returns null.
     */
    fun getValidToken(context: Context): String? {
        return if (isTokenExpired(context)) null else getToken(context)
    }
}
