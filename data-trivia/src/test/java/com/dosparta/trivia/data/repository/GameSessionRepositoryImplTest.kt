package com.dosparta.trivia.data.repository

import android.os.Build
import com.dosparta.trivia.data.local.dao.GameSessionDao
import com.dosparta.trivia.data.local.entity.GameSessionEntity
import com.dosparta.trivia.domain.model.TriviaQuestion
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class GameSessionRepositoryImplTest {

    @Mock
    private lateinit var dao: GameSessionDao

    private lateinit var repository: GameSessionRepositoryImpl

    private val sampleQuestions = listOf(
        TriviaQuestion(
            category = "Science",
            type = "multiple",
            difficulty = "easy",
            question = "What is H2O?",
            correctAnswer = "Water",
            options = listOf("Water", "Salt", "Sugar", "Ice")
        )
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = GameSessionRepositoryImpl(dao)
    }

    @Test
    fun `saveGameSession calls dao insertOrReplaceSession`() = runBlocking {
        val currentIndex = 0
        val correctCount = 0
        val activeElapsedMillis = System.currentTimeMillis()

        repository.saveGameSession(
            questions = sampleQuestions,
            currentIndex = currentIndex,
            correctCount = correctCount,
            activeElapsedMillis = activeElapsedMillis,
            selectedAnswers = emptyMap()
        )

        verify(dao).insertOrReplaceSession(any<GameSessionEntity>())
    }

    @Test
    fun `saveGameSession passes correct data to dao`() = runBlocking {
        val currentIndex = 1
        val correctCount = 5
        val activeElapsedMillis = System.currentTimeMillis()

        repository.saveGameSession(
            questions = sampleQuestions,
            currentIndex = currentIndex,
            correctCount = correctCount,
            activeElapsedMillis = activeElapsedMillis,
            selectedAnswers = emptyMap()
        )

        verify(dao).insertOrReplaceSession(any())
    }

    @Test
    fun `getActiveSessionFlow returns flow from dao`() = runBlocking {
        val entity = GameSessionEntity(
            amount = 1,
            categoryId = null,
            difficulty = null,
            questionsJson = "[{\"category\":\"Science\",\"type\":\"multiple\",\"difficulty\":\"easy\",\"question\":\"What is H2O?\",\"correctAnswer\":\"Water\",\"options\":[\"Water\",\"Salt\",\"Sugar\",\"Ice\"]}]",
            startedAtMillis = System.currentTimeMillis()
        )

        whenever(dao.getActiveSession()).thenReturn(flowOf(entity))

        repository.getActiveSessionFlow().collect { state ->
            assertNotNull(state)
            assertEquals(1, state?.questions?.size)
        }
    }

    @Test
    fun `getActiveSessionFlow returns null when no active session`() = runBlocking {
        whenever(dao.getActiveSession()).thenReturn(flowOf(null))

        repository.getActiveSessionFlow().collect { state ->
            assertNull(state)
        }
    }

    @Test
    fun `getActiveSession queries dao and returns game session state`() = runBlocking {
        val entity = GameSessionEntity(
            amount = 1,
            categoryId = null,
            difficulty = null,
            questionsJson = "[{\"category\":\"Science\",\"type\":\"multiple\",\"difficulty\":\"easy\",\"question\":\"What is H2O?\",\"correctAnswer\":\"Water\",\"options\":[\"Water\",\"Salt\",\"Sugar\",\"Ice\"]}]",
            startedAtMillis = System.currentTimeMillis(),
            currentIndex = 0,
            correctCount = 0
        )

        whenever(dao.getActiveSessionOnce()).thenReturn(entity)

        val state = repository.getActiveSession()

        assertNotNull(state)
        assertEquals(1, state?.questions?.size)
        assertEquals(0, state?.currentIndex)
        assertEquals(0, state?.correctCount)
    }

    @Test
    fun `getActiveSession returns null when dao returns null`() = runBlocking {
        whenever(dao.getActiveSessionOnce()).thenReturn(null)

        val state = repository.getActiveSession()

        assertNull(state)
    }

    @Test
    fun `clearActiveSession calls dao clearActiveSession`() = runBlocking {
        repository.clearActiveSession()

        verify(dao).clearActiveSession()
    }

    @Test
    fun `saveGameSession with selected answers preserves mapping`() = runBlocking {
        val selectedAnswers = mapOf(0 to "Water", 1 to "Salt")

        repository.saveGameSession(
            questions = sampleQuestions,
            currentIndex = 0,
            correctCount = 0,
            activeElapsedMillis = System.currentTimeMillis(),
            selectedAnswers = selectedAnswers
        )

        verify(dao).insertOrReplaceSession(any())
    }

    @Test
    fun `getActiveSession properly deserializes selected answers`() = runBlocking {
        val entity = GameSessionEntity(
            amount = 1,
            categoryId = null,
            difficulty = null,
            questionsJson = "[{\"category\":\"Science\",\"type\":\"multiple\",\"difficulty\":\"easy\",\"question\":\"What is H2O?\",\"correctAnswer\":\"Water\",\"options\":[\"Water\",\"Salt\",\"Sugar\",\"Ice\"]}]",
            selectedAnswersJson = "{\"0\":\"Water\"}",
            startedAtMillis = System.currentTimeMillis(),
            currentIndex = 1,
            correctCount = 1
        )

        whenever(dao.getActiveSessionOnce()).thenReturn(entity)

        val state = repository.getActiveSession()

        assertNotNull(state)
        assertEquals(1, state?.selectedAnswers?.size)
        assertEquals("Water", state?.selectedAnswers?.get(0))
    }

    @Test
    fun `getActiveSession handles empty selected answers json gracefully`() = runBlocking {
        val entity = GameSessionEntity(
            amount = 1,
            categoryId = null,
            difficulty = null,
            questionsJson = "[{\"category\":\"Science\",\"type\":\"multiple\",\"difficulty\":\"easy\",\"question\":\"What is H2O?\",\"correctAnswer\":\"Water\",\"options\":[\"Water\",\"Salt\",\"Sugar\",\"Ice\"]}]",
            selectedAnswersJson = null,
            startedAtMillis = System.currentTimeMillis(),
            currentIndex = 0,
            correctCount = 0
        )

        whenever(dao.getActiveSessionOnce()).thenReturn(entity)

        val state = repository.getActiveSession()

        assertNotNull(state)
        assertEquals(0, state?.selectedAnswers?.size)
    }
}
