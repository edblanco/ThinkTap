package com.dosparta.trivia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.dosparta.trivia.ui.UiText
import com.dosparta.trivia.ui.components.ErrorScreen
import com.dosparta.trivia.ui.preview.PreviewFixtures
import com.dosparta.trivia.ui.preview.TriviaPreviewTheme

@Composable
fun LoadingScreen(
    error: UiText?,
    onRetry: () -> Unit
) {
    Scaffold { innerPadding ->
        if (error != null) {
            ErrorScreen(
                message = error,
                onRetry = onRetry
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(28.dp),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(28.dp))
                repeat(4) {
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    )
}

@PreviewLightDark
@Composable
private fun LoadingSkeletonPreview() {
    TriviaPreviewTheme {
        LoadingScreen(error = null, onRetry = {})
    }
}

@PreviewLightDark
@Composable
private fun LoadingErrorPreview() {
    TriviaPreviewTheme {
        LoadingScreen(error = PreviewFixtures.error, onRetry = {})
    }
}

@Preview(widthDp = 320, heightDp = 80)
@Composable
private fun SkeletonBlockPreview() {
    TriviaPreviewTheme {
        SkeletonBlock(modifier = Modifier.padding(12.dp).fillMaxWidth().height(56.dp))
    }
}
