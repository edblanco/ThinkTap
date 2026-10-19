package com.dosparta.triviagame2.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.dosparta.triviagame2.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    @ApplicationContext context: Context
) : AuthRepository {
    private val mutableState = MutableStateFlow<AuthState>(AuthState.Restoring)
    override val state: StateFlow<AuthState> = mutableState.asStateFlow()
    private val credentials = CredentialManager.create(context)
    private val webClientId = context.getString(R.string.google_web_client_id)
    private val firebase = createFirebase(context)

    init {
        if (firebase == null) {
            Log.e(TAG, "Pro login requires Firebase and Google web client configuration; see docs/authentication.md")
            mutableState.value = AuthState.ConfigurationError
        } else {
            firebase.addAuthStateListener { auth ->
                val user = auth.currentUser
                mutableState.value = if (user == null) AuthState.SignedOut
                    else AuthState.SignedIn(user.uid, user.displayName)
            }
        }
    }

    override suspend fun signIn(activityContext: Context) {
        val auth = firebase ?: throw AuthException(AuthError.CONFIGURATION)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()
        val credential = try {
            credentials.getCredential(activityContext, request).credential
        } catch (_: GetCredentialCancellationException) {
            return
        } catch (e: GetCredentialException) {
            throw AuthException(AuthError.PROVIDER, e)
        }
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw AuthException(AuthError.CREDENTIAL)
        }
        val googleToken = try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            throw AuthException(AuthError.CREDENTIAL, e)
        }
        try {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(googleToken, null)).await()
        } catch (e: FirebaseNetworkException) {
            throw AuthException(AuthError.NETWORK, e)
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Firebase sign-in rejected (${e.errorCode})")
            throw AuthException(AuthError.CREDENTIAL, e)
        } catch (e: FirebaseException) {
            throw AuthException(AuthError.PROVIDER, e)
        }
    }

    override suspend fun signOut() {
        // Firebase logout must succeed even if the account chooser's state cannot be cleared.
        firebase?.signOut()
        try {
            credentials.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: ClearCredentialException) {
            throw AuthException(AuthError.SESSION_CLEAR, e)
        }
    }

    override suspend fun refreshSession() {
        val auth = firebase ?: return
        val user = auth.currentUser ?: return
        try {
            user.reload().await()
        } catch (e: FirebaseAuthInvalidUserException) {
            if (auth.currentUser?.uid == user.uid) auth.signOut()
            Log.w(TAG, "Firebase session is no longer valid (${e.errorCode})")
        } catch (e: FirebaseNetworkException) {
            throw AuthException(AuthError.NETWORK, e)
        } catch (e: FirebaseAuthException) {
            throw AuthException(AuthError.CREDENTIAL, e)
        } catch (e: FirebaseException) {
            throw AuthException(AuthError.PROVIDER, e)
        }
    }

    private fun createFirebase(context: Context): FirebaseAuth? {
        val apiKey = context.getString(R.string.firebase_api_key)
        val applicationId = context.getString(R.string.firebase_application_id)
        val projectId = context.getString(R.string.firebase_project_id)
        if (listOf(apiKey, applicationId, projectId, webClientId).any { it.isBlank() }) return null
        val options = FirebaseOptions.Builder()
            .setApiKey(apiKey)
            .setApplicationId(applicationId)
            .setProjectId(projectId)
            .build()
        return try {
            val app = FirebaseApp.getApps(context).firstOrNull { it.name == FIREBASE_APP }
                ?: FirebaseApp.initializeApp(context, options, FIREBASE_APP)
            FirebaseAuth.getInstance(app)
        } catch (_: IllegalArgumentException) {
            Log.e(TAG, "Invalid Firebase client configuration")
            null
        }
    }

    private companion object {
        const val TAG = "ProAuthentication"
        const val FIREBASE_APP = "thinktap-pro"
    }
}
