package com.dosparta.triviagame2.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    val state = repository.state
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableSigningOut = MutableStateFlow(false)
    val signingOut = mutableSigningOut.asStateFlow()
    private val mutableError = MutableStateFlow<AuthError?>(null)
    val error = mutableError.asStateFlow()
    private var refreshJob: Job? = null

    fun signIn(context: Context) = runOperation { repository.signIn(context) }

    fun signOut(preserveGame: suspend () -> Unit) = runOperation(leavingGame = true) {
        withContext(NonCancellable) {
            preserveGame()
            repository.signOut()
        }
    }

    fun refreshSession() {
        if (state.value !is AuthState.SignedIn || mutableBusy.value || refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            try {
                repository.refreshSession()
                mutableError.value = null
            } catch (e: AuthException) {
                Log.w(TAG, "Session refresh failed: ${e.error}")
                mutableError.value = e.error
            }
        }
    }

    private fun runOperation(leavingGame: Boolean = false, operation: suspend () -> Unit) {
        if (mutableBusy.value) return
        refreshJob?.cancel()
        mutableBusy.value = true
        mutableSigningOut.value = leavingGame
        mutableError.value = null
        viewModelScope.launch {
            try {
                operation()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthException) {
                Log.e(TAG, "Authentication operation failed: ${e.error}")
                mutableError.value = e.error
            } finally {
                mutableBusy.value = false
                mutableSigningOut.value = false
            }
        }
    }

    private companion object {
        const val TAG = "ProAuthentication"
    }
}
