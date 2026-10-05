package com.dosparta.trivia.sdk

import com.dosparta.trivia.domain.localization.IContentLocalizationRepository
import com.dosparta.trivia.domain.model.AppLanguage
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import com.dosparta.trivia.domain.repository.ITriviaRepository
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class NetworkFailureTest {
    @Test
    fun `JVM adapters retain IOException classification and cause`() = runTest {
        val failure = IOException("offline")
        val repository = object : ITriviaRepository {
            override suspend fun getQuestions(config: TriviaConfig): List<TriviaQuestion> = throw failure
            override suspend fun getCategories(): List<TriviaCategory> = throw failure
        }
        val localization = object : IContentLocalizationRepository {
            override val contentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
            override val translationUnavailable = MutableStateFlow(false)
            override fun setContentLanguage(language: AppLanguage) { contentLanguage.value = language }
            override fun setTranslationUnavailable(unavailable: Boolean) { translationUnavailable.value = unavailable }
        }
        val sdk = TriviaSdk(repository, mockk<IGameSessionRepository>(), localization)
        sdk.start()
        val startFailure = (sdk.state.value as TriviaState.Failed).failure
        assertEquals(TriviaError.NETWORK, startFailure.error)
        assertSame(failure, startFailure.cause)
        val categoryFailure = assertFailsWith<TriviaSdkException> { sdk.loadCategories() }
        assertEquals(TriviaError.NETWORK, categoryFailure.error)
        assertSame(failure, categoryFailure.cause)
    }
}
