package com.dosparta.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette: an indigo primary for focus and trust, a warm amber accent for scores and
// celebration, and a teal tertiary for supporting highlights.

private val Indigo10 = Color(0xFF14134A)
private val Indigo20 = Color(0xFF262476)
private val Indigo30 = Color(0xFF3835A4)
private val Indigo40 = Color(0xFF4F46E5)
private val Indigo80 = Color(0xFFC3C4FF)
private val Indigo90 = Color(0xFFE2E0FF)

private val Amber10 = Color(0xFF2B1700)
private val Amber20 = Color(0xFF472A00)
private val Amber30 = Color(0xFF663E00)
private val Amber40 = Color(0xFFB4740A)
private val Amber80 = Color(0xFFFFB955)
private val Amber90 = Color(0xFFFFDDB0)

private val Teal10 = Color(0xFF00201C)
private val Teal20 = Color(0xFF00382F)
private val Teal30 = Color(0xFF005046)
private val Teal40 = Color(0xFF0D8074)
private val Teal80 = Color(0xFF6FDBC8)
private val Teal90 = Color(0xFF8FF8E3)

private val Neutral10 = Color(0xFF1A1B20)
private val Neutral90 = Color(0xFFE4E1E9)
private val NeutralVariant30 = Color(0xFF45464F)
private val NeutralVariant80 = Color(0xFFC6C5D0)

internal val BrandLightColorScheme = lightColorScheme(
    primary = Indigo40,
    onPrimary = Color.White,
    primaryContainer = Indigo90,
    onPrimaryContainer = Indigo10,
    inversePrimary = Indigo80,
    secondary = Amber40,
    onSecondary = Color.White,
    secondaryContainer = Amber90,
    onSecondaryContainer = Amber10,
    tertiary = Teal40,
    onTertiary = Color.White,
    tertiaryContainer = Teal90,
    onTertiaryContainer = Teal10,
    background = Color(0xFFFCF8FF),
    onBackground = Neutral10,
    surface = Color(0xFFFCF8FF),
    onSurface = Neutral10,
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = NeutralVariant30,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F2FA),
    surfaceContainer = Color(0xFFF0ECF4),
    surfaceContainerHigh = Color(0xFFEAE7EF),
    surfaceContainerHighest = Color(0xFFE5E1E9),
    surfaceTint = Indigo40,
    inverseSurface = Color(0xFF2F3036),
    inverseOnSurface = Color(0xFFF2EFF7),
    outline = Color(0xFF767680),
    outlineVariant = NeutralVariant80,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color.Black
)

internal val BrandDarkColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = Indigo20,
    primaryContainer = Indigo30,
    onPrimaryContainer = Indigo90,
    inversePrimary = Indigo40,
    secondary = Amber80,
    onSecondary = Amber20,
    secondaryContainer = Amber30,
    onSecondaryContainer = Amber90,
    tertiary = Teal80,
    onTertiary = Teal20,
    tertiaryContainer = Teal30,
    onTertiaryContainer = Teal90,
    background = Color(0xFF121318),
    onBackground = Neutral90,
    surface = Color(0xFF121318),
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1A1B20),
    surfaceContainer = Color(0xFF1E1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
    surfaceTint = Indigo80,
    inverseSurface = Neutral90,
    inverseOnSurface = Color(0xFF2F3036),
    outline = Color(0xFF90909A),
    outlineVariant = NeutralVariant30,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black
)

/**
 * Quiz-specific color roles that Material's standard scheme does not model.
 *
 * Material 3 defines an `error` role but no "success" role, so correct-answer feedback previously
 * relied on hardcoded hex values that ignored the active theme and lost contrast in dark mode.
 * These roles are resolved per theme instead.
 */
@Suppress("LongParameterList")
data class QuizColors(
    val correct: Color,
    val onCorrect: Color,
    val correctContainer: Color,
    val onCorrectContainer: Color,
    val incorrect: Color,
    val onIncorrect: Color,
    val incorrectContainer: Color,
    val onIncorrectContainer: Color,
    val timerTrack: Color,
    val timerIndicator: Color
)

internal val LightQuizColors = QuizColors(
    correct = Color(0xFF17683C),
    onCorrect = Color.White,
    correctContainer = Color(0xFFA5F5BF),
    onCorrectContainer = Color(0xFF00210E),
    incorrect = Color(0xFFBA1A1A),
    onIncorrect = Color.White,
    incorrectContainer = Color(0xFFFFDAD6),
    onIncorrectContainer = Color(0xFF410002),
    timerTrack = Color(0xFFE3E1EC),
    timerIndicator = Indigo40
)

internal val DarkQuizColors = QuizColors(
    correct = Color(0xFF89D9A5),
    onCorrect = Color(0xFF00391C),
    correctContainer = Color(0xFF00522B),
    onCorrectContainer = Color(0xFFA5F5BF),
    incorrect = Color(0xFFFFB4AB),
    onIncorrect = Color(0xFF690005),
    incorrectContainer = Color(0xFF93000A),
    onIncorrectContainer = Color(0xFFFFDAD6),
    timerTrack = Color(0xFF34343A),
    timerIndicator = Indigo80
)
