package com.alertyai.app.ui.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Helper that wraps the Android Credential Manager Google Sign-In flow.
 */
object GoogleSignInHelper {

    /**
     * Your Google OAuth 2.0 Web Client ID (NOT the Android client ID).
     * Must match the GOOGLE_CLIENT_ID the backend uses.
     */
    const val WEB_CLIENT_ID = "868948779906-4mutbtvje0p271c7fbg2rpnogvag1si3.apps.googleusercontent.com"

    /**
     * Sign in with Google and return the Google ID Token as a Result.
     *
     * @param context The Activity context (not Application context)
     */
    suspend fun getIdToken(context: Context): Result<String> {
        return try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            when {
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    Result.success(googleCredential.idToken)
                }
                else -> Result.failure(Exception("Unexpected credential type: ${credential.type}"))
            }

        } catch (e: NoCredentialException) {
            Result.failure(Exception("No Google account found. Add one in Android Settings."))
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Login cancelled."))
        } catch (e: GetCredentialException) {
            Result.failure(Exception("Login failed: ${e.localizedMessage}"))
        } catch (e: GoogleIdTokenParsingException) {
            Result.failure(Exception("Token parse error: ${e.localizedMessage}"))
        }
    }

    /**
     * Clears the credential state (signs out of Google session on device).
     */
    suspend fun signOut(context: Context) {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            // Log or ignore
        }
    }
}
