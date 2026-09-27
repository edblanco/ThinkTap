package com.dosparta.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalInspectionMode
import com.dosparta.core.ui.theme.LocalReducedMotion
import com.dosparta.core.ui.theme.TriviaMotion

private const val SHIMMER_TRAVEL = 1_200f
private const val SHIMMER_BAND_WIDTH = 400f
private const val BASE_ALPHA = 0.55f
private const val HIGHLIGHT_ALPHA = 0.22f

/**
 * A placeholder block with a highlight that sweeps across it, used while content loads.
 *
 * The sweep is skipped under reduced motion and in previews, where it degrades to a plain tinted
 * block so the layout still reads correctly.
 */
@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small
) {
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = BASE_ALPHA)
    val animate = !LocalReducedMotion.current && !LocalInspectionMode.current

    if (!animate) {
        Box(modifier = modifier.clip(shape).background(base))
        return
    }

    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = HIGHLIGHT_ALPHA)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -SHIMMER_BAND_WIDTH,
        targetValue = SHIMMER_TRAVEL,
        animationSpec = infiniteRepeatable(
            animation = tween(TriviaMotion.SHIMMER_PERIOD_MILLIS),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset, 0f),
        end = Offset(offset + SHIMMER_BAND_WIDTH, 0f)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(base)
            .background(brush)
    )
}
