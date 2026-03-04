package com.alertyai.app.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages JWT token storage in SharedPreferences.
 * Uses MODE_PRIVATE — acceptable for dev; upgrade to EncryptedSharedPreferences
 * once security-crypto is stable on the target devices.
 */
object TokenManager {
    private const val PREF_FILE = "alertyai_prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_EMAIL = "user_email"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    fun saveToken(context: Context, token: String, email: String = "") {
        getPrefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun getToken(context: Context): String? = getPrefs(context).getString(KEY_TOKEN, null)

    fun getEmail(context: Context): String = getPrefs(context).getString(KEY_EMAIL, "") ?: ""

    fun clearToken(context: Context) = getPrefs(context).edit().clear().apply()

    fun isLoggedIn(context: Context): Boolean = !getToken(context).isNullOrBlank()
}
