package com.dosparta.triviagame2.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dosparta.core.ui.components.TriviaScreenScaffold
import com.dosparta.triviagame2.R

@Composable
fun AuthScreen(
    loading: Boolean,
    busy: Boolean,
    configured: Boolean,
    error: String?,
    onSignIn: () -> Unit
) {
    TriviaScreenScaffold(title = stringResource(R.string.app_name)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).testTag("pro_login"),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(if (loading) R.string.auth_restoring else R.string.auth_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            if (loading || busy) {
                CircularProgressIndicator(Modifier.testTag("auth_loading"))
            }
            if (!loading) {
                Text(stringResource(R.string.auth_description), textAlign = TextAlign.Center)
                Button(
                    onClick = onSignIn,
                    enabled = configured && !busy,
                    modifier = Modifier.testTag("google_sign_in")
                ) {
                    Text(stringResource(R.string.auth_google_sign_in))
                }
            }
            if (!configured) AuthErrorText(stringResource(R.string.auth_error_configuration))
            if (error != null) AuthErrorText(error)
        }
    }
}

@Composable
fun AccountBar(displayName: String?, error: String?, enabled: Boolean, onSignOut: () -> Unit) {
    Surface {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    displayName ?: stringResource(R.string.auth_signed_in),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge
                )
                TextButton(
                    onClick = onSignOut,
                    enabled = enabled,
                    modifier = Modifier.testTag("pro_sign_out")
                ) {
                    Text(stringResource(R.string.auth_sign_out))
                }
            }
            if (error != null) AuthErrorText(error)
        }
    }
}

@Composable
private fun AuthErrorText(message: String) {
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(8.dp).semantics { liveRegion = LiveRegionMode.Polite }
    )
}

@Composable
fun AuthError.errorString(): String = stringResource(
    when (this) {
        AuthError.CONFIGURATION -> R.string.auth_error_configuration
        AuthError.NETWORK -> R.string.auth_error_network
        AuthError.CREDENTIAL -> R.string.auth_error_credential
        AuthError.PROVIDER -> R.string.auth_error_provider
        AuthError.SESSION_CLEAR -> R.string.auth_error_session_clear
    }
)
