package com.dosparta.triviagame2.auth

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

sealed interface AuthState {
    data object Restoring : AuthState
    data object SignedOut : AuthState
    data object ConfigurationError : AuthState
    data class SignedIn(val uid: String, val displayName: String?) : AuthState
}

enum class AuthError {
    CONFIGURATION, NETWORK, CREDENTIAL, PROVIDER, SESSION_CLEAR
}

class AuthException(val error: AuthError, cause: Throwable? = null) : Exception(error.name, cause)

interface AuthRepository {
    val state: StateFlow<AuthState>
    suspend fun signIn(activityContext: Context)
    suspend fun signOut()
    suspend fun refreshSession()
}
