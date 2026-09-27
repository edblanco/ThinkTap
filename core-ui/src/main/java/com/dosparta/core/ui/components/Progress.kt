package com.dosparta.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.unit.dp
import com.dosparta.core.ui.theme.TriviaMotion

private val TrackHeight = 10.dp

/**
 * A rounded progress bar that animates between values.
 *
 * Unlike the Material progress indicator this fills with a gradient, which keeps long quizzes
 * feeling like they are building towards something.
 *
 * @param progress the fraction complete, coerced into `0f..1f`
 * @param contentDescription describes the progress for screen readers
 */
@Composable
fun TriviaProgressBar(
    progress: Float,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = TriviaMotion.spatial(),
        label = "progress"
    )
    val scheme = MaterialTheme.colorScheme
    val fill = Brush.horizontalGradient(listOf(scheme.primary, scheme.tertiary))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TrackHeight)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(scheme.surfaceVariant)
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(fill)
        )
    }
}

/**
 * A horizontal bar that fills over a fixed duration, used to show how long is left before an
 * action happens automatically.
 *
 * @param progress how much of the countdown has elapsed, in `0f..1f`
 * @param color the fill color
 */
@Composable
fun CountdownFill(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = TriviaMotion.effects(),
        label = "countdownColor"
    )
    Box(
        modifier = modifier
            .fillMaxWidth(progress.coerceIn(0f, 1f))
            .fillMaxHeight()
            .background(animatedColor)
    )
}
