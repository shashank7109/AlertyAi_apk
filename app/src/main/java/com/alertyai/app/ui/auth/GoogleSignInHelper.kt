package com.alertyai.app.ui.auth

import android.content.Context
import android.os.CancellationSignal
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
     * Common errors:
     *  - NoCredentialException  → no Google account on device, or app not registered in Play Console
     *  - Cancellation           → user dismissed the picker
     *
     * @param context The Activity context (not Application context — needed for the picker UI)
     */
    suspend fun getIdToken(context: Context): Result<String> {
        return try {
            val credentialManager = CredentialManager.create(context)

            // First try: only previously authorized accounts (faster UX)
            // Falls back to all accounts if none are found
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)  // show all accounts on device
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
            // Most common on emulators — no Google account signed in
            Result.failure(
                Exception(
                    "No Google account found on this device.\n\n" +
                    "Please go to Settings → Accounts → Add account → Google, " +
                    "then try again."
                )
            )
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Google sign-in was cancelled."))
        } catch (e: GetCredentialException) {
            Result.failure(
                Exception(
                    "Google sign-in failed. Make sure:\n" +
                    "• A Google account is added to this device\n" +
                    "• Google Play Services is up to date\n\n" +
                    "Error: ${e.localizedMessage}"
                )
            )
        } catch (e: GoogleIdTokenParsingException) {
            Result.failure(Exception("Could not read Google credentials: ${e.localizedMessage}"))
        }
    }
}
