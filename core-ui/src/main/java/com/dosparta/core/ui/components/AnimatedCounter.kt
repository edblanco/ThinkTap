package com.dosparta.core.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import com.dosparta.core.ui.theme.TriviaMotion

/**
 * Displays [value] as a number that counts up to its target with a bouncy settle.
 *
 * The animation is decorative, so the node exposes [contentDescription] with the final value and
 * hides the intermediate numbers from screen readers.
 *
 * @param value the target number
 * @param contentDescription how the value should be announced
 */
@Composable
fun AnimatedCounter(
    value: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = TriviaMotion.celebratory(),
        label = "counter"
    )
    Text(
        text = animated.toString(),
        style = style,
        color = color,
        modifier = modifier.clearAndSetSemantics {
            this.contentDescription = contentDescription
        }
    )
}
