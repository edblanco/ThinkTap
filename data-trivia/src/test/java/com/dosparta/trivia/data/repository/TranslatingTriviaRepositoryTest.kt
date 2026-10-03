package com.dosparta.trivia.data.repository

import android.os.Build
import com.dosparta.trivia.data.localization.ContentLocalizationRepositoryImpl
import com.dosparta.trivia.data.localization.TriviaContentTranslator
import com.dosparta.trivia.domain.localization.ITextTranslator
import com.dosparta.trivia.domain.model.AppLanguage
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import kotlinx.coroutines.test.runTest
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class TranslatingTriviaRepositoryTest {

    private val question = TriviaQuestion(
        category = "Science",
        type = "multiple",
        difficulty = "easy",
        question = "What is H2O?",
        correctAnswer = "Water",
        options = listOf("Water", "Salt")
    )
    private val categories = listOf(TriviaCategory(17, "Science"))
    private val config = TriviaConfig(amount = 10)

    private val delegate: TriviaRepositoryImpl = mockk()
    private val localization = ContentLocalizationRepositoryImpl()

    private fun repository(translator: ITextTranslator) =
        TranslatingTriviaRepository(delegate, TriviaContentTranslator(translator), localization)

    private val prefixingTranslator = object : ITextTranslator {
        override suspend fun translate(texts: List<String>, target: AppLanguage) =
            texts.map { "${target.tag}:$it" }
    }

    private val failingTranslator = object : ITextTranslator {
        override suspend fun translate(texts: List<String>, target: AppLanguage): List<String> =
            throw IOException("model download failed")
    }

    @Test
    fun `translates questions into the content language`() = runTest {
        coEvery { delegate.getQuestions(config) } returns listOf(question)
        localization.setContentLanguage(AppLanguage.GERMAN)

        val result = repository(prefixingTranslator).getQuestions(config).single()

        assertEquals("de:What is H2O?", result.question)
        assertEquals("de:Water", result.correctAnswer)
        assertFalse(localization.translationUnavailable.value)
    }

    @Test
    fun `falls back to english and flags the notice when translation fails`() = runTest {
        coEvery { delegate.getCategories() } returns categories
        localization.setContentLanguage(AppLanguage.SPANISH)

        val result = repository(failingTranslator).getCategories()

        assertEquals(categories, result)
        assertTrue(localization.translationUnavailable.value)
    }

    @Test
    fun `english content clears a previous failure notice`() = runTest {
        coEvery { delegate.getCategories() } returns categories
        localization.setTranslationUnavailable(true)

        val result = repository(failingTranslator).getCategories()

        assertEquals(categories, result)
        assertFalse(localization.translationUnavailable.value)
    }

    @Test
    fun `successful translation clears a previous failure notice`() = runTest {
        coEvery { delegate.getCategories() } returns categories
        localization.setContentLanguage(AppLanguage.CHINESE_SIMPLIFIED)
        localization.setTranslationUnavailable(true)

        val result = repository(prefixingTranslator).getCategories()

        assertEquals("zh-CN:Science", result.single().name)
        assertFalse(localization.translationUnavailable.value)
    }
}
