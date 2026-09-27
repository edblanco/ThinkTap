package com.dosparta.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.dosparta.core.ui.theme.TriviaMotion

private const val PRESSED_SCALE = 0.96f

/**
 * Scales the content down slightly while [interactionSource] reports a press.
 *
 * The scale is spring-driven rather than tween-driven so releasing mid-press settles naturally
 * instead of snapping, and it collapses to no animation under reduced motion.
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = PRESSED_SCALE
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = TriviaMotion.spatialFast(),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Haptic vocabulary for the app, kept in one place so feedback stays consistent across screens. */
object TriviaHaptics {

    /** A normal, reversible tap such as selecting an option. */
    fun HapticFeedback.tap() = performHapticFeedback(HapticFeedbackType.ContextClick)

    /** A committing action such as confirming an answer or starting a game. */
    fun HapticFeedback.confirm() = performHapticFeedback(HapticFeedbackType.Confirm)

    /** An incorrect answer. */
    fun HapticFeedback.reject() = performHapticFeedback(HapticFeedbackType.Reject)

    /** A celebratory moment such as finishing a game. */
    fun HapticFeedback.success() = performHapticFeedback(HapticFeedbackType.LongPress)
}

/** Returns the ambient [HapticFeedback] for use with the [TriviaHaptics] helpers. */
@Composable
fun rememberTriviaHaptics(): HapticFeedback = LocalHapticFeedback.current
