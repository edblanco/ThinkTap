package com.dosparta.trivia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.dosparta.core.ui.components.BrandBackground
import com.dosparta.core.ui.components.ShimmerBlock
import com.dosparta.trivia.ui.UiText
import com.dosparta.trivia.ui.components.ErrorScreen
import com.dosparta.trivia.ui.preview.PreviewFixtures
import com.dosparta.trivia.ui.preview.TriviaPreviewTheme

private const val SKELETON_ROW_COUNT = 4

/**
 * The first screen shown while the app decides whether to resume a saved game.
 *
 * It renders a skeleton of the screen that follows rather than a spinner, so the transition into
 * real content does not shift the layout.
 *
 * @param error a startup error to show instead of the skeleton, if any
 * @param onRetry retries startup after an error
 */
@Composable
fun StartupScreen(
    error: UiText?,
    onRetry: () -> Unit
) {
    if (error != null) {
        ErrorScreen(message = error, onRetry = onRetry)
        return
    }

    BrandBackground(modifier = Modifier.testTag("startup_screen")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(28.dp),
                shape = MaterialTheme.shapes.medium
            )
            repeat(SKELETON_ROW_COUNT) {
                ShimmerBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = MaterialTheme.shapes.large
                )
            }
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun StartupSkeletonPreview() {
    TriviaPreviewTheme {
        StartupScreen(error = null, onRetry = {})
    }
}

@PreviewLightDark
@Composable
private fun StartupErrorPreview() {
    TriviaPreviewTheme {
        StartupScreen(error = PreviewFixtures.error, onRetry = {})
    }
}
