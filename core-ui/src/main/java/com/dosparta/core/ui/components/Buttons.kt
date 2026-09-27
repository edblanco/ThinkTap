package com.dosparta.core.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dosparta.core.ui.components.TriviaHaptics.confirm
import com.dosparta.core.ui.components.TriviaHaptics.tap

private val ButtonMinHeight = 56.dp

/**
 * The app's primary call to action: a filled, fully rounded button that scales on press and
 * confirms with haptic feedback.
 */
@Composable
fun TriviaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable (RowScope.() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = rememberTriviaHaptics()
    Button(
        onClick = {
            haptics.confirm()
            onClick()
        },
        modifier = modifier
            .defaultMinSize(minHeight = ButtonMinHeight)
            .pressScale(interactionSource),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp
        )
    ) {
        if (content != null) {
            content()
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * A lower-emphasis action rendered as an outlined button, matching [TriviaPrimaryButton]'s
 * height, shape and press behaviour.
 */
@Composable
fun TriviaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = rememberTriviaHaptics()
    OutlinedButton(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = modifier
            .defaultMinSize(minHeight = ButtonMinHeight)
            .pressScale(interactionSource),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        interactionSource = interactionSource
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading?.invoke()
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A text-only tertiary action, for example "skip" or "not now". */
@Composable
fun TriviaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = rememberTriviaHaptics()
    TextButton(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = modifier.pressScale(interactionSource),
        interactionSource = interactionSource
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
