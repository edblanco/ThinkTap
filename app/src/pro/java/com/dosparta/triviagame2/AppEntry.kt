package com.dosparta.triviagame2

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.dosparta.trivia.ui.asString
import com.dosparta.trivia.ui.viewmodel.TriviaViewModel
import com.dosparta.triviagame2.auth.AccountBar
import com.dosparta.triviagame2.auth.AuthScreen
import com.dosparta.triviagame2.auth.AuthState
import com.dosparta.triviagame2.auth.AuthViewModel
import com.dosparta.triviagame2.auth.errorString

@Composable
fun AppEntry(viewModel: TriviaViewModel, content: @Composable () -> Unit) {
    val auth: AuthViewModel = composeViewModel()
    ProAppEntry(viewModel, auth, content)
}

@Composable
fun ProAppEntry(
    viewModel: TriviaViewModel,
    auth: AuthViewModel,
    content: @Composable () -> Unit
) {
    val state by auth.state.collectAsStateWithLifecycle()
    val busy by auth.busy.collectAsStateWithLifecycle()
    val signingOut by auth.signingOut.collectAsStateWithLifecycle()
    val error by auth.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var preparedState by remember { mutableStateOf<AuthState?>(null) }
    val persistenceError by viewModel.persistenceError.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        val previousUser = preparedState as? AuthState.SignedIn
        preparedState = null
        val currentUser = state as? AuthState.SignedIn
        if (currentUser != null) {
            if (previousUser != null && previousUser.uid != currentUser.uid) {
                viewModel.suspendForAuthentication()
            }
            viewModel.enableAfterAuthentication()
            auth.refreshSession()
        } else {
            viewModel.suspendForAuthentication()
        }
        preparedState = state
    }
    ObserveSessionResume(auth)

    val authError = error?.errorString()
    val message = authError ?: persistenceError?.asString()
    val user = state as? AuthState.SignedIn
    if (user != null && preparedState == state && !signingOut) {
        key(user.uid) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                AccountBar(
                    displayName = user.displayName,
                    error = message,
                    enabled = !busy,
                    onSignOut = {
                        auth.signOut {
                            viewModel.suspendForAuthentication()
                        }
                    }
                )
                Box(Modifier.weight(1f)) { content() }
            }
        }
    } else {
        AuthScreen(
            loading = state == AuthState.Restoring || preparedState != state || signingOut,
            busy = busy,
            configured = state != AuthState.ConfigurationError,
            error = message,
            onSignIn = { auth.signIn(context) }
        )
    }
}

@Composable
private fun ObserveSessionResume(auth: AuthViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, auth) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                auth.refreshSession()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
