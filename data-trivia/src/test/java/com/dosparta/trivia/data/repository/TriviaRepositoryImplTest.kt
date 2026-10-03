package com.dosparta.trivia.data.repository

import android.os.Build
import com.dosparta.trivia.data.remote.api.TriviaApi
import com.dosparta.trivia.data.remote.dto.TriviaQuestionDto
import com.dosparta.trivia.data.remote.dto.TriviaResponseDto
import com.dosparta.trivia.data.token.TriviaSessionTokenProvider
import com.dosparta.trivia.domain.model.TriviaConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class TriviaRepositoryImplTest {

    @MockK
    private lateinit var api: TriviaApi

    @MockK
    private lateinit var tokenProvider: TriviaSessionTokenProvider

    private lateinit var repository: TriviaRepositoryImpl
    private lateinit var sampleDto: TriviaQuestionDto
    private lateinit var sampleResponse: TriviaResponseDto
    private lateinit var sampleConfig: TriviaConfig

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
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
        coEvery { tokenProvider.getValidToken() } returns "token-a"
        coEvery { api.fetchQuestions(eq(10), null, null, eq("token-a")) } returns sampleResponse

        val domainList = repository.getQuestions(sampleConfig)

        assertEquals(1, domainList.size)
        val domain = domainList[0]
        assertEquals("mul&tiple", domain.type)
        assertEquals("eas<y", domain.difficulty)
        assertEquals("Math & Science", domain.category)
        assertEquals("What does <HTML> stand for?", domain.question)
        coVerify { tokenProvider.markTokenUsed() }
    }

    @Test
    fun `getQuestions passes selected config and token to api`() {
        runBlocking {
            val config = TriviaConfig(amount = 20, categoryId = 9, difficulty = "easy")
            coEvery { tokenProvider.getValidToken() } returns "token-b"
            coEvery { api.fetchQuestions(20, 9, "easy", "token-b") } returns sampleResponse

            repository.getQuestions(config)

            coVerify { api.fetchQuestions(20, 9, "easy", "token-b") }
        }
    }

    @Test
    fun `getQuestions retries with fresh token when token not found`() {
        runBlocking {
            coEvery { tokenProvider.getValidToken() } returnsMany listOf("token-old", "token-new")
            coEvery { api.fetchQuestions(eq(10), null, null, eq("token-old")) } returns
                TriviaResponseDto(responseCode = 3, results = emptyList())
            coEvery { api.fetchQuestions(eq(10), null, null, eq("token-new")) } returns sampleResponse

            repository.getQuestions(sampleConfig)

            coVerify { tokenProvider.clearToken() }
            coVerify { api.fetchQuestions(10, null, null, "token-old") }
            coVerify { api.fetchQuestions(10, null, null, "token-new") }
        }
    }

    @Test
    fun `getQuestions resets token when token empty`() {
        runBlocking {
            coEvery { tokenProvider.getValidToken() } returns "token-a"
            coEvery { tokenProvider.resetToken("token-a") } returns "token-reset"
            coEvery { api.fetchQuestions(eq(10), null, null, eq("token-a")) } returns
                TriviaResponseDto(responseCode = 4, results = emptyList())
            coEvery { api.fetchQuestions(eq(10), null, null, eq("token-reset")) } returns sampleResponse

            repository.getQuestions(sampleConfig)

            coVerify { tokenProvider.resetToken("token-a") }
            coVerify { api.fetchQuestions(10, null, null, "token-reset") }
        }
    }

    @Test
    fun `getQuestions throws helpful error for no results code`() = runBlocking {
        coEvery { tokenProvider.getValidToken() } returns "token-a"
        coEvery { api.fetchQuestions(eq(10), isNull(), isNull(), eq("token-a")) } returns
            TriviaResponseDto(responseCode = 1, results = emptyList())

        try {
            repository.getQuestions(sampleConfig)
            fail("Expected exception not thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("no results", ignoreCase = true))
        }
    }

    @Test
    fun `getQuestions throws helpful error for invalid parameter code`() = runBlocking {
        coEvery { tokenProvider.getValidToken() } returns "token-a"
        coEvery { api.fetchQuestions(eq(10), isNull(), isNull(), eq("token-a")) } returns
            TriviaResponseDto(responseCode = 2, results = emptyList())

        try {
            repository.getQuestions(sampleConfig)
            fail("Expected exception not thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("rejected query", ignoreCase = true))
        }
    }

    @Test
    fun `getQuestions throws helpful error for rate limit code`() = runBlocking {
        coEvery { tokenProvider.getValidToken() } returns "token-a"
        coEvery { api.fetchQuestions(eq(10), isNull(), isNull(), eq("token-a")) } returns
            TriviaResponseDto(responseCode = 5, results = emptyList())

        try {
            repository.getQuestions(sampleConfig)
            fail("Expected exception not thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("rate limit", ignoreCase = true))
        }
    }

    @Test
    fun `getQuestions propagates API exceptions`() = runBlocking {
        coEvery { tokenProvider.getValidToken() } returns "token-a"
        coEvery { api.fetchQuestions(eq(10), isNull(), isNull(), eq("token-a")) } throws
            RuntimeException("API error")

        try {
            repository.getQuestions(sampleConfig)
            fail("Expected exception not thrown")
        } catch (e: Exception) {
            assertEquals("API error", e.message)
        }
    }

    @Test
    fun `getQuestions calls token only once when successful`() = runBlocking {
        coEvery { tokenProvider.getValidToken() } returns "token-a"
        coEvery { api.fetchQuestions(eq(10), isNull(), isNull(), eq("token-a")) } returns sampleResponse

        repository.getQuestions(sampleConfig)

        coVerify(exactly = 1) { tokenProvider.getValidToken() }
        coVerify(exactly = 1) { tokenProvider.markTokenUsed() }
    }
}
