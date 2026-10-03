package com.dosparta.trivia.data.mapper

import android.os.Build
import com.dosparta.trivia.domain.model.TriviaQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class GameSessionMapperTest {

    private val sampleQuestions = listOf(
        TriviaQuestion(
            category = "Science",
            type = "multiple",
            difficulty = "easy",
            question = "What is H2O?",
            correctAnswer = "Water",
            options = listOf("Water", "Salt", "Sugar", "Ice")
        ),
        TriviaQuestion(
            category = "History",
            type = "boolean",
            difficulty = "medium",
            question = "Was Rome founded in 753 BC?",
            correctAnswer = "True",
            options = listOf("True", "False")
        )
    )

    @Test
    fun `toEntity converts GameSession data to GameSessionEntity correctly`() {
        val currentIndex = 1
        val correctCount = 5
        val activeElapsedMillis = System.currentTimeMillis()
        val selectedAnswers = mapOf(0 to "Water", 1 to "False")

        val entity = GameSessionMapper.toEntity(
            questions = sampleQuestions,
            currentIndex = currentIndex,
            correctCount = correctCount,
            activeElapsedMillis = activeElapsedMillis,
            selectedAnswers = selectedAnswers
        )

        assertEquals("active_game_session", entity.sessionId)
        assertEquals(2, entity.amount)
        assertEquals(null, entity.categoryId)
        assertEquals(null, entity.difficulty)
        assertEquals(currentIndex, entity.currentIndex)
        assertEquals(correctCount, entity.correctCount)
        assertEquals(activeElapsedMillis, entity.startedAtMillis)
        assertEquals("in_progress", entity.status)
        assertNotNull(entity.questionsJson)
        assertNotNull(entity.selectedAnswersJson)
    }

    @Test
    fun `fromEntity converts GameSessionEntity back to GameSessionState correctly`() {
        val currentIndex = 1
        val correctCount = 5
        val activeElapsedMillis = System.currentTimeMillis()
        val selectedAnswers = mapOf(0 to "Water", 1 to "False")

        val entity = GameSessionMapper.toEntity(
            questions = sampleQuestions,
            currentIndex = currentIndex,
            correctCount = correctCount,
            activeElapsedMillis = activeElapsedMillis,
            selectedAnswers = selectedAnswers
        )

        val state = GameSessionMapper.fromEntity(entity)

        assertEquals(2, state.questions.size)
        assertEquals(sampleQuestions[0].question, state.questions[0].question)
        assertEquals(sampleQuestions[1].question, state.questions[1].question)
        assertEquals(currentIndex, state.currentIndex)
        assertEquals(correctCount, state.correctCount)
        assertEquals(activeElapsedMillis, state.activeElapsedMillis)
        assertEquals(2, state.selectedAnswers.size)
        assertEquals("Water", state.selectedAnswers[0])
        assertEquals("False", state.selectedAnswers[1])
    }

    @Test
    fun `roundtrip conversion preserves all data accurately`() {
        val currentIndex = 0
        val correctCount = 0
        val activeElapsedMillis = System.currentTimeMillis()
        val selectedAnswers = emptyMap<Int, String>()

        val entity = GameSessionMapper.toEntity(
            questions = sampleQuestions,
            currentIndex = currentIndex,
            correctCount = correctCount,
            activeElapsedMillis = activeElapsedMillis,
            selectedAnswers = selectedAnswers
        )
        val state = GameSessionMapper.fromEntity(entity)

        assertEquals(sampleQuestions.size, state.questions.size)
        assertEquals(currentIndex, state.currentIndex)
        assertEquals(correctCount, state.correctCount)
        assertEquals(activeElapsedMillis, state.activeElapsedMillis)
        assertEquals(0, state.selectedAnswers.size)
    }

    @Test
    fun `handles empty selected answers gracefully`() {
        val entity = GameSessionMapper.toEntity(
            questions = sampleQuestions,
            currentIndex = 0,
            correctCount = 0,
            activeElapsedMillis = System.currentTimeMillis(),
            selectedAnswers = emptyMap()
        )

        val state = GameSessionMapper.fromEntity(entity)

        assertEquals(0, state.selectedAnswers.size)
    }

    @Test
    fun `handles single question correctly`() {
        val singleQuestion = listOf(sampleQuestions[0])

        val entity = GameSessionMapper.toEntity(
            questions = singleQuestion,
            currentIndex = 0,
            correctCount = 0,
            activeElapsedMillis = System.currentTimeMillis(),
            selectedAnswers = emptyMap()
        )

        val state = GameSessionMapper.fromEntity(entity)

        assertEquals(1, state.questions.size)
        assertEquals(singleQuestion[0].question, state.questions[0].question)
    }

    @Test
    fun `preserves question metadata through serialization`() {
        val entity = GameSessionMapper.toEntity(
            questions = sampleQuestions,
            currentIndex = 1,
            correctCount = 1,
            activeElapsedMillis = System.currentTimeMillis(),
            selectedAnswers = emptyMap()
        )

        val state = GameSessionMapper.fromEntity(entity)

        // Verify first question metadata
        assertEquals("Science", state.questions[0].category)
        assertEquals("multiple", state.questions[0].type)
        assertEquals("easy", state.questions[0].difficulty)
        assertEquals("Water", state.questions[0].correctAnswer)
        assertEquals(4, state.questions[0].options.size)

        // Verify second question metadata
        assertEquals("History", state.questions[1].category)
        assertEquals("boolean", state.questions[1].type)
        assertEquals("medium", state.questions[1].difficulty)
        assertEquals("True", state.questions[1].correctAnswer)
        assertEquals(2, state.questions[1].options.size)
    }
}
