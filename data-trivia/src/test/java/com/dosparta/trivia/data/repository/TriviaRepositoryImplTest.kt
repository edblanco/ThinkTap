package com.dosparta.trivia.data.repository

import android.os.Build
import com.dosparta.trivia.data.remote.api.TriviaApi
import com.dosparta.trivia.data.remote.dto.TriviaQuestionDto
import com.dosparta.trivia.data.remote.dto.TriviaResponseDto
import com.dosparta.trivia.data.token.TriviaSessionTokenProvider
import com.dosparta.trivia.domain.model.TriviaConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class TriviaRepositoryImplTest {

    @Mock
    private lateinit var api: TriviaApi

    @Mock
    private lateinit var tokenProvider: TriviaSessionTokenProvider

    private lateinit var repository: TriviaRepositoryImpl
    private lateinit var sampleDto: TriviaQuestionDto
    private lateinit var sampleResponse: TriviaResponseDto
    private lateinit var sampleConfig: TriviaConfig

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        sampleDto = TriviaQuestionDto(
            type = "mul&amp;tiple",
            difficulty = "eas&lt;y",
            category = "Math &amp; Science",
            question = "What does &lt;HTML&gt; stand for?",
            correctAnswer = "HyperText Markup Language",
            incorrectAnswers = listOf(
                "Hyperlinks and Text Markup Language",
                "Home Tool Markup Language",
                "Hyperlinking Text Markup Language"
            )
        )
        sampleResponse = TriviaResponseDto(
            responseCode = 0,
            results = listOf(sampleDto)
        )
        sampleConfig = TriviaConfig(amount = 10)
        repository = TriviaRepositoryImpl(api, tokenProvider, Dispatchers.Unconfined)
    }

    @Test
    fun `getQuestions decodes HTML entities and marks token used`() = runBlocking {
        whenever(tokenProvider.getValidToken()).thenReturn("token-a")
        whenever(api.fetchQuestions(eq(10), eq(null), eq(null), eq("token-a")))
            .thenReturn(sampleResponse)

        val domainList = repository.getQuestions(sampleConfig)

        assertEquals(1, domainList.size)
        val domain = domainList[0]
        assertEquals("mul&tiple", domain.type)
        assertEquals("eas<y", domain.difficulty)
        assertEquals("Math & Science", domain.category)
        assertEquals("What does <HTML> stand for?", domain.question)
        verify(tokenProvider).markTokenUsed()
    }

    @Test
    fun `getQuestions passes selected config and token to api`() {
        runBlocking {
            val config = TriviaConfig(amount = 20, categoryId = 9, difficulty = "easy")
            whenever(tokenProvider.getValidToken()).thenReturn("token-b")
            whenever(api.fetchQuestions(20, 9, "easy", "token-b")).thenReturn(sampleResponse)

            repository.getQuestions(config)

            verify(api).fetchQuestions(20, 9, "easy", "token-b")
        }
    }

    @Test
    fun `getQuestions retries with fresh token when token not found`() {
        runBlocking {
            whenever(tokenProvider.getValidToken()).thenReturn("token-old", "token-new")
            whenever(api.fetchQuestions(eq(10), eq(null), eq(null), eq("token-old")))
                .thenReturn(TriviaResponseDto(responseCode = 3, results = emptyList()))
            whenever(api.fetchQuestions(eq(10), eq(null), eq(null), eq("token-new")))
                .thenReturn(sampleResponse)

            repository.getQuestions(sampleConfig)

            verify(tokenProvider).clearToken()
            verify(api).fetchQuestions(10, null, null, "token-old")
            verify(api).fetchQuestions(10, null, null, "token-new")
        }
    }

    @Test
    fun `getQuestions resets token when token empty`() {
        runBlocking {
            whenever(tokenProvider.getValidToken()).thenReturn("token-a")
            whenever(tokenProvider.resetToken("token-a")).thenReturn("token-reset")
            whenever(api.fetchQuestions(eq(10), eq(null), eq(null), eq("token-a")))
                .thenReturn(TriviaResponseDto(responseCode = 4, results = emptyList()))
            whenever(api.fetchQuestions(eq(10), eq(null), eq(null), eq("token-reset")))
                .thenReturn(sampleResponse)

            repository.getQuestions(sampleConfig)

            verify(tokenProvider).resetToken("token-a")
            verify(api).fetchQuestions(10, null, null, "token-reset")
        }
    }

    @Test
    fun `getQuestions throws helpful error for no results code`() = runBlocking {
        whenever(tokenProvider.getValidToken()).thenReturn("token-a")
        whenever(api.fetchQuestions(eq(10), isNull(), isNull(), eq("token-a")))
            .thenReturn(TriviaResponseDto(responseCode = 1, results = emptyList()))

        try {
            repository.getQuestions(sampleConfig)
            fail("Expected exception not thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("no results", ignoreCase = true))
        }
    }

    @Test
    fun `getQuestions throws helpful error for invalid parameter code`() = runBlocking {
        whenever(tokenProvider.getValidToken()).thenReturn("token-a")
        whenever(api.fetchQuestions(eq(10), isNull(), isNull(), eq("token-a")))
            .thenReturn(TriviaResponseDto(responseCode = 2, results = emptyList()))

        try {
            repository.getQuestions(sampleConfig)
            fail("Expected exception not thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("rejected query", ignoreCase = true))
        }
    }

    @Test
    fun `getQuestions throws helpful error for rate limit code`() = runBlocking {
        whenever(tokenProvider.getValidToken()).thenReturn("token-a")
        whenever(api.fetchQuestions(eq(10), isNull(), isNull(), eq("token-a")))
            .thenReturn(TriviaResponseDto(responseCode = 5, results = emptyList()))

        try {
            repository.getQuestions(sampleConfig)
            fail("Expected exception not thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("rate limit", ignoreCase = true))
        }
    }

    @Test
    fun `getQuestions propagates API exceptions`() = runBlocking {
        whenever(tokenProvider.getValidToken()).thenReturn("token-a")
        whenever(api.fetchQuestions(eq(10), isNull(), isNull(), eq("token-a")))
            .thenThrow(RuntimeException("API error"))

        try {
            repository.getQuestions(sampleConfig)
            fail("Expected exception not thrown")
        } catch (e: Exception) {
            assertEquals("API error", e.message)
        }
    }

    @Test
    fun `getQuestions calls token only once when successful`() = runBlocking {
        whenever(tokenProvider.getValidToken()).thenReturn("token-a")
        whenever(api.fetchQuestions(eq(10), isNull(), isNull(), eq("token-a"))).thenReturn(sampleResponse)

        repository.getQuestions(sampleConfig)

        verify(tokenProvider, times(1)).getValidToken()
        verify(tokenProvider, times(1)).markTokenUsed()
    }
}
