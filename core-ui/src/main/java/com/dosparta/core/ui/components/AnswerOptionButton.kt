package com.dosparta.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.dosparta.core.ui.theme.LocalReducedMotion
import com.dosparta.core.ui.theme.TriviaMotion
import com.dosparta.core.ui.theme.quizColors

private val OptionMinHeight = 60.dp
private val IconSize = 22.dp
private const val DIMMED_ALPHA = 0.45f
private const val REVEAL_POP_SCALE = 1.04f

/** How an answer option should be rendered once the user has answered. */
enum class AnswerVisualState {
    /** No answer chosen yet, or this option is not part of the outcome. */
    Unanswered,

    /** This option is the correct answer. */
    Correct,

    /** This option was chosen and is wrong. */
    Incorrect,

    /** Another option was chosen; this one is de-emphasised. */
    Dimmed
}

/**
 * A tappable answer option that animates into its result state.
 *
 * Color, a result icon and a brief scale pop all change together when [state] resolves, so the
 * outcome is conveyed by more than color alone.
 *
 * @param text the answer text
 * @param state how the option should look
 * @param onClick invoked when the option is tapped; ignored when [enabled] is false
 */
@Composable
fun AnswerOptionButton(
    text: String,
    state: AnswerVisualState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val quiz = MaterialTheme.quizColors
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }

    val targetContainer = when (state) {
        AnswerVisualState.Correct -> quiz.correctContainer
        AnswerVisualState.Incorrect -> quiz.incorrectContainer
        else -> scheme.surfaceContainerHigh
    }
    val targetContent = when (state) {
        AnswerVisualState.Correct -> quiz.onCorrectContainer
        AnswerVisualState.Incorrect -> quiz.onIncorrectContainer
        else -> scheme.onSurface
    }

    val container by animateColorAsState(
        targetValue = targetContainer,
        animationSpec = TriviaMotion.effects(),
        label = "answerContainer"
    )
    val content by animateColorAsState(
        targetValue = targetContent,
        animationSpec = TriviaMotion.effects(),
        label = "answerContent"
    )
    val alpha by animateFloatAsState(
        targetValue = if (state == AnswerVisualState.Dimmed) DIMMED_ALPHA else 1f,
        animationSpec = TriviaMotion.effects(),
        label = "answerAlpha"
    )

    val pop = rememberResultPop(state)

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = OptionMinHeight)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = pop
                scaleY = pop
            }
            .pressScale(interactionSource),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            ResultIcon(state = state, tint = content)
        }
    }
}

@Composable
private fun ResultIcon(state: AnswerVisualState, tint: Color) {
    val icon = when (state) {
        AnswerVisualState.Correct -> Icons.Filled.Check
        AnswerVisualState.Incorrect -> Icons.Filled.Close
        else -> null
    }
    Box(modifier = Modifier.size(IconSize)) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
        }
    }
}

/**
 * Returns a scale that briefly pops above 1 when [state] resolves to a result, then settles back.
 */
@Composable
private fun rememberResultPop(state: AnswerVisualState): Float {
    val reducedMotion = LocalReducedMotion.current
    val spec = TriviaMotion.spatialFast<Float>()
    val scale = remember { Animatable(1f) }
    val isResult = state == AnswerVisualState.Correct || state == AnswerVisualState.Incorrect

    LaunchedEffect(state, reducedMotion) {
        if (reducedMotion || !isResult) {
            scale.snapTo(1f)
        } else {
            scale.animateTo(REVEAL_POP_SCALE, animationSpec = spec)
            scale.animateTo(1f, animationSpec = spec)
        }
    }
    return scale.value
}
