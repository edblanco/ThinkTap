package com.dosparta.trivia.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.dosparta.core.ui.components.BrandBackground
import com.dosparta.core.ui.theme.LocalReducedMotion
import com.dosparta.trivia.ui.R
import com.dosparta.trivia.ui.preview.TriviaPreviewTheme

private const val DOT_COUNT = 3
private const val DOT_CYCLE_MILLIS = 900
private const val DOT_MIN_SCALE = 0.5f

/**
 * A full-screen loading indicator.
 */
@Composable
fun LoadingScreen() {
    BrandBackground(modifier = Modifier.testTag("loading_screen")) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PulsingDots()
            Text(
                text = stringResource(R.string.loading_questions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }
}

/**
 * Three dots that pulse in sequence.
 *
 * Used instead of a spinner because it echoes the app's rounded shape language, and it collapses
 * to three static dots when motion is reduced.
 */
@Composable
private fun PulsingDots(modifier: Modifier = Modifier) {
    val animate = !LocalReducedMotion.current && !LocalInspectionMode.current
    val transition = rememberInfiniteTransition(label = "dots")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(DOT_COUNT) { index ->
            val scale = if (animate) {
                val animated by transition.animateFloat(
                    initialValue = DOT_MIN_SCALE,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(DOT_CYCLE_MILLIS),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(index * DOT_CYCLE_MILLIS / DOT_COUNT)
                    ),
                    label = "dot$index"
                )
                animated
            } else {
                1f
            }

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun LoadingScreenPreview() {
    TriviaPreviewTheme {
        LoadingScreen()
    }
}

@Preview(widthDp = 200, heightDp = 80)
@Composable
private fun PulsingDotsPreview() {
    TriviaPreviewTheme {
        PulsingDots(modifier = Modifier.padding(16.dp))
    }
}
