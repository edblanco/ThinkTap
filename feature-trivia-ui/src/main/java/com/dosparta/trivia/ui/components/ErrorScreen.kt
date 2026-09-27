package com.dosparta.trivia.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.dosparta.core.ui.components.BrandBackground
import com.dosparta.core.ui.components.EnterAnimated
import com.dosparta.core.ui.components.TriviaCard
import com.dosparta.core.ui.components.TriviaPrimaryButton
import com.dosparta.trivia.ui.R
import com.dosparta.trivia.ui.UiText
import com.dosparta.trivia.ui.asString
import com.dosparta.trivia.ui.preview.PreviewFixtures
import com.dosparta.trivia.ui.preview.TriviaPreviewTheme

/**
 * A full-screen error display with a retry action.
 *
 * @param message the error message to show
 * @param onRetry callback triggered when the user taps retry
 */
@Composable
fun ErrorScreen(
    message: UiText,
    onRetry: () -> Unit
) {
    BrandBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EnterAnimated {
                TriviaCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = message.asString(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            EnterAnimated(modifier = Modifier.padding(top = 24.dp), index = 1) {
                TriviaPrimaryButton(
                    text = stringResource(R.string.retry),
                    onClick = onRetry
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ErrorScreenPreview() {
    TriviaPreviewTheme {
        ErrorScreen(message = PreviewFixtures.error, onRetry = {})
    }
}
