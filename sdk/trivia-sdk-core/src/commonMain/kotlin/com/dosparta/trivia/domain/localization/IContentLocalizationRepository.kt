package com.dosparta.trivia.domain.localization

import com.dosparta.trivia.domain.model.AppLanguage
import kotlinx.coroutines.flow.StateFlow

/**
 * Tracks which language trivia content should be delivered in and whether the latest
 * translation attempt had to fall back to the original English content.
 */
interface IContentLocalizationRepository {
    val contentLanguage: StateFlow<AppLanguage>

    /** True when the most recently loaded content could not be translated and is shown in English. */
    val translationUnavailable: StateFlow<Boolean>

    fun setContentLanguage(language: AppLanguage)

    fun setTranslationUnavailable(unavailable: Boolean)
}
