package com.dosparta.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dosparta.core.ui.theme.LocalReducedMotion
import com.dosparta.core.ui.theme.TriviaMotion
import kotlinx.coroutines.delay

private val DefaultRise = 24.dp

/**
 * Fades and lifts [content] into place the first time it is composed.
 *
 * @param index position among siblings; later items start slightly later so a group enters as a
 *   wave rather than all at once
 * @param key re-runs the entrance when it changes, for example when a new question is shown
 */
@Composable
fun EnterAnimated(
    modifier: Modifier = Modifier,
    index: Int = 0,
    key: Any? = Unit,
    rise: Dp = DefaultRise,
    content: @Composable ColumnScope.() -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val staggerDelay = TriviaMotion.staggerDelay(index).toLong()
    val spec = TriviaMotion.spatial<Float>()
    val risePx = with(LocalDensity.current) { rise.toPx() }
    val progress = remember(key, index) { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(key, index, reducedMotion) {
        if (reducedMotion) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            delay(staggerDelay)
            progress.animateTo(1f, animationSpec = spec)
        }
    }

    Column(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * risePx
        },
        content = content
    )
}
