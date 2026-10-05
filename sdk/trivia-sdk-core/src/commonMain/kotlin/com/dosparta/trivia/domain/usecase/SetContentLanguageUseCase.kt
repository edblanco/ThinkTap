package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.localization.IContentLocalizationRepository
import com.dosparta.trivia.domain.model.AppLanguage

/**
 * Updates the language trivia content is translated into.
 *
 * @return true if the language actually changed
 */
internal class SetContentLanguageUseCase(
    private val repository: IContentLocalizationRepository
) {
    operator fun invoke(language: AppLanguage): Boolean {
        if (repository.contentLanguage.value == language) return false
        repository.setContentLanguage(language)
        return true
    }
}
