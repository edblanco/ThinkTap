package com.dosparta.trivia.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.dosparta.trivia.ui.preview.TriviaPreviewTheme

@PreviewLightDark
@Composable
private fun LoadingScreenPreview() {
    TriviaPreviewTheme {
        LoadingScreen()
    }
}

/**
 * A full-screen loading indicator.
 */
@Composable
fun LoadingScreen() {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("loading_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

    }
}
