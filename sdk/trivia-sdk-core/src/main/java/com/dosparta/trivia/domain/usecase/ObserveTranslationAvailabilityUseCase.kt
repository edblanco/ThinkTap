package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.localization.IContentLocalizationRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Exposes whether the latest trivia content fell back to English because translation failed.
 */
internal class ObserveTranslationAvailabilityUseCase @Inject constructor(
    private val repository: IContentLocalizationRepository
) {
    operator fun invoke(): StateFlow<Boolean> = repository.translationUnavailable
}
