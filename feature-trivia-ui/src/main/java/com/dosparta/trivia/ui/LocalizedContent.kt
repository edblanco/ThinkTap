package com.dosparta.trivia.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.dosparta.trivia.domain.model.AppLanguage
import com.dosparta.trivia.domain.model.TriviaQuestion

/** The language the UI is currently rendered in, as resolved by Android's resource system. */
@Composable
@ReadOnlyComposable
internal fun currentContentLanguage(): AppLanguage =
    AppLanguage.fromTag(stringResource(R.string.content_language_tag)) ?: AppLanguage.DEFAULT

@StringRes
internal fun AppLanguage.displayNameRes(): Int = when (this) {
    AppLanguage.ENGLISH -> R.string.language_name_english
    AppLanguage.GERMAN -> R.string.language_name_german
    AppLanguage.SPANISH -> R.string.language_name_spanish
    AppLanguage.CHINESE_SIMPLIFIED -> R.string.language_name_chinese_simplified
}

/** Localized label for an OpenTDB difficulty value (`easy`, `medium`, `hard`). */
@Composable
@ReadOnlyComposable
internal fun localizedDifficulty(difficulty: String): String =
    when (difficulty.trim().lowercase()) {
        "easy" -> stringResource(R.string.difficulty_easy)
        "medium" -> stringResource(R.string.difficulty_medium)
        "hard" -> stringResource(R.string.difficulty_hard)
        else -> difficulty.replaceFirstChar { it.uppercase() }
    }

/**
 * Display text for an answer option. Boolean questions keep OpenTDB's English "True"/"False"
 * values (so answer matching is unaffected) and are only localized for display.
 */
@Composable
@ReadOnlyComposable
internal fun localizedAnswer(question: TriviaQuestion, option: String): String {
    if (!question.type.equals("boolean", ignoreCase = true)) return option
    return when (option.trim().lowercase()) {
        "true" -> stringResource(R.string.answer_true)
        "false" -> stringResource(R.string.answer_false)
        else -> option
    }
}
