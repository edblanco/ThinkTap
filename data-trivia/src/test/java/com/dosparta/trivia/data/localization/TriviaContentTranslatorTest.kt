package com.dosparta.trivia.data.localization

import com.dosparta.trivia.domain.localization.ITextTranslator
import com.dosparta.trivia.domain.model.AppLanguage
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaQuestion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TriviaContentTranslatorTest {

    private class RecordingTranslator(
        private val transform: (String) -> String = { "de:$it" }
    ) : ITextTranslator {
        val requests = mutableListOf<List<String>>()
        override suspend fun translate(texts: List<String>, target: AppLanguage): List<String> {
            requests += texts
            return texts.map(transform)
        }
    }

    private val multiple = TriviaQuestion(
        category = "Science",
        type = "multiple",
        difficulty = "easy",
        question = "What is H2O?",
        correctAnswer = "Water",
        options = listOf("Salt", "Water", "Sugar", "Ice")
    )
    private val boolean = TriviaQuestion(
        category = "Science",
        type = "boolean",
        difficulty = "easy",
        question = "Is the sky blue?",
        correctAnswer = "True",
        options = listOf("True", "False")
    )

    @Test
    fun `english target returns content untouched without translating`() = runTest {
        val translator = RecordingTranslator()
        val questions = listOf(multiple)

        val result = TriviaContentTranslator(translator).translateQuestions(questions, AppLanguage.ENGLISH)

        assertSame(questions, result)
        assertTrue(translator.requests.isEmpty())
    }

    @Test
    fun `translates question text and keeps correct answer among options`() = runTest {
        val translator = RecordingTranslator()

        val result = TriviaContentTranslator(translator)
            .translateQuestions(listOf(multiple), AppLanguage.GERMAN)
            .single()

        assertEquals("de:Science", result.category)
        assertEquals("de:What is H2O?", result.question)
        assertEquals(listOf("de:Salt", "de:Water", "de:Sugar", "de:Ice"), result.options)
        assertEquals("de:Water", result.correctAnswer)
        assertTrue(result.correctAnswer in result.options)
        assertEquals("easy", result.difficulty)
    }

    @Test
    fun `boolean answers stay untranslated for consistent matching`() = runTest {
        val result = TriviaContentTranslator(RecordingTranslator())
            .translateQuestions(listOf(boolean), AppLanguage.SPANISH)
            .single()

        assertEquals("de:Is the sky blue?", result.question)
        assertEquals(listOf("True", "False"), result.options)
        assertEquals("True", result.correctAnswer)
    }

    @Test
    fun `each distinct string is translated once per batch`() = runTest {
        val translator = RecordingTranslator()

        TriviaContentTranslator(translator)
            .translateQuestions(listOf(multiple, multiple.copy(question = "Other?")), AppLanguage.GERMAN)

        val sent = translator.requests.single()
        assertEquals(sent.distinct(), sent)
    }

    @Test
    fun `keeps english options when translations collapse into duplicates`() = runTest {
        val translator = RecordingTranslator { if (it == "Salt" || it == "Water") "same" else "de:$it" }

        val result = TriviaContentTranslator(translator)
            .translateQuestions(listOf(multiple), AppLanguage.GERMAN)
            .single()

        assertEquals(multiple.options, result.options)
        assertEquals(multiple.correctAnswer, result.correctAnswer)
        assertEquals("de:What is H2O?", result.question)
    }

    @Test
    fun `translates category names and keeps ids`() = runTest {
        val categories = listOf(TriviaCategory(9, "General Knowledge"), TriviaCategory(17, "Science"))

        val result = TriviaContentTranslator(RecordingTranslator())
            .translateCategories(categories, AppLanguage.CHINESE_SIMPLIFIED)

        assertEquals(listOf(9, 17), result.map { it.id })
        assertEquals(listOf("de:General Knowledge", "de:Science"), result.map { it.name })
    }
}
