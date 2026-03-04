package com.timetogo.app.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.timetogo.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Repository for Google Sign-In using Credential Manager API.
 */
open class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    data class SignInResult(
        val success: Boolean,
        val name: String = "",
        val email: String = "",
        val errorMessage: String = "",
        val noAccountFound: Boolean = false
    )

    /**
     * Attempt Google Sign-In using Credential Manager.
     * Uses GetSignInWithGoogleOption which shows Google's own sign-in button UI.
     * This is more reliable than GetGoogleIdOption as it doesn't require
     * pre-existing credentials on the device.
     *
     * @param activityContext The Activity context required for Credential Manager UI
     */
    open suspend fun signIn(activityContext: Context): SignInResult {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty()) {
            Log.e(TAG, "GOOGLE_WEB_CLIENT_ID is not set in local.properties")
            return SignInResult(
                success = false,
                errorMessage = "Google Sign-In is not configured. Please set GOOGLE_WEB_CLIENT_ID in local.properties."
            )
        }

        Log.d(TAG, "Starting sign-in with client ID: ${BuildConfig.GOOGLE_WEB_CLIENT_ID.take(20)}...")

        return try {
            // Use GetSignInWithGoogleOption — this shows Google's own sign-in UI
            // and is more reliable than GetGoogleIdOption which depends on
            // pre-existing credential state on the device.
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            ).build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            Log.d(TAG, "Launching Credential Manager request...")
            val result = credentialManager.getCredential(activityContext, request)
            Log.d(TAG, "Got credential response, type: ${result.credential.type}")
            handleSignInResult(result)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Sign-in cancelled by user", e)
            SignInResult(success = false, errorMessage = "Sign-in was cancelled.")
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No credentials available: ${e.message}", e)
            SignInResult(success = false, errorMessage = "No Google account found on this device.", noAccountFound = true)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Sign-in failed — type: ${e.type}, message: ${e.message}", e)
            SignInResult(success = false, errorMessage = "Sign-in failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected sign-in error: ${e.javaClass.simpleName}: ${e.message}", e)
            SignInResult(success = false, errorMessage = "An unexpected error occurred: ${e.message}")
        }
    }

    /**
     * Attempt a silent sign-in (auto-select a previously authorized account).
     */
    open suspend fun silentSignIn(activityContext: Context): SignInResult {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty()) {
            return SignInResult(success = false, errorMessage = "Client ID not configured.")
        }

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            handleSignInResult(result)
        } catch (e: Exception) {
            Log.d(TAG, "Silent sign-in not available: ${e.javaClass.simpleName}: ${e.message}")
            SignInResult(success = false, errorMessage = "")
        }
    }

    /**
     * Sign out the current user by clearing credential state.
     */
    open suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing credential state", e)
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse): SignInResult {
        val credential = result.credential
        Log.d(TAG, "Credential type: ${credential.type}")

        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            Log.d(TAG, "Sign-in successful for: ${googleIdTokenCredential.id}")
            return SignInResult(
                success = true,
                name = googleIdTokenCredential.displayName ?: "",
                email = googleIdTokenCredential.id
            )
        }
        Log.w(TAG, "Unexpected credential type: ${credential.type}")
        return SignInResult(success = false, errorMessage = "Unexpected credential type received.")
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
