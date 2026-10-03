package com.dosparta.trivia.domain.localization

import com.dosparta.trivia.domain.model.AppLanguage

/**
 * Translates English text into one of the supported [AppLanguage]s.
 */
interface ITextTranslator {
    /**
     * Translates [texts] from English into [target], preserving order.
     * @throws Exception when the translation model is unavailable or translation fails.
     */
    suspend fun translate(texts: List<String>, target: AppLanguage): List<String>
}
