package com.dosparta.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val LocalQuizColors = staticCompositionLocalOf { LightQuizColors }

/**
 * The app theme.
 *
 * Applies the brand color, type and shape scales, plus the app's motion tokens and quiz-specific
 * color roles.
 *
 * @param darkTheme whether to use the dark palette
 * @param dynamicColor when true, Android 12+ wallpaper colors replace the brand palette. This is
 *   off by default: the quiz has its own visual identity, and dynamic color would otherwise
 *   discard both hand-tuned schemes on most devices.
 * @param content the themed content
 */
@Composable
fun TriviaGame2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = resolveColorScheme(darkTheme = darkTheme, dynamicColor = dynamicColor)
    val quizColors = if (darkTheme) DarkQuizColors else LightQuizColors

    CompositionLocalProvider(
        LocalQuizColors provides quizColors,
        LocalReducedMotion provides rememberSystemReducedMotion()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = TriviaShapes,
            typography = TriviaTypography,
            content = content
        )
    }
}

@Composable
private fun resolveColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return when {
        dynamicColor && supportsDynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> BrandDarkColorScheme
        else -> BrandLightColorScheme
    }
}

/** Quiz-specific color roles for the current theme, such as correct and incorrect feedback. */
val MaterialTheme.quizColors: QuizColors
    @Composable
    @ReadOnlyComposable
    get() = LocalQuizColors.current
