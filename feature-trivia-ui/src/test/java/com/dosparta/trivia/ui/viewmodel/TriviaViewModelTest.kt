package com.dosparta.trivia.ui.viewmodel

import android.util.Log
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.domain.game.GameSession
import com.dosparta.trivia.domain.model.AppLanguage
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.sdk.TriviaError
import com.dosparta.trivia.sdk.TriviaSdk
import com.dosparta.trivia.sdk.TriviaSdkException
import com.dosparta.trivia.sdk.TriviaState
import com.dosparta.trivia.ui.R
import com.dosparta.trivia.ui.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TriviaViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val sdk = mockk<TriviaSdk>(relaxed = true)
    private val state = MutableStateFlow<TriviaState>(TriviaState.Idle)
    private val persistenceFailure = MutableStateFlow<TriviaSdkException?>(null)
    private val translationUnavailable = MutableStateFlow(false)
    private lateinit var viewModel: TriviaViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any(), any<Throwable>()) } returns 0
        every { sdk.state } returns state
        every { sdk.persistenceFailure } returns persistenceFailure
        every { sdk.translationUnavailable } returns translationUnavailable
        coEvery { sdk.restore() } returns false
        coEvery { sdk.loadCategories() } returns emptyList()
        viewModel = TriviaViewModel(sdk)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `SDK states map to presentation without implementing game rules`() = runTest {
        runCurrent()
        state.value = TriviaState.Loading
        runCurrent()
        assertEquals(TriviaUiState.Loading, viewModel.uiState.value)
        val session = GameSession(emptyList())
        state.value = TriviaState.Playing(session)
        runCurrent()
        assertEquals(TriviaUiState.Game(session), viewModel.uiState.value)
        val result = GameResult(10, 8, 100)
        state.value = TriviaState.Finished(result)
        runCurrent()
        assertEquals(TriviaUiState.Result(result), viewModel.uiState.value)
    }

    @Test
    fun `typed SDK failures map to localized messages`() = runTest {
        runCurrent()
        val mappings = mapOf(
            TriviaError.INVALID_CONFIGURATION to R.string.error_invalid_amount,
            TriviaError.NO_QUESTIONS to R.string.error_no_questions_available,
            TriviaError.NO_RESULTS to R.string.error_no_results_for_filters,
            TriviaError.INVALID_QUERY to R.string.error_invalid_query_parameters,
            TriviaError.RATE_LIMIT to R.string.error_rate_limit,
            TriviaError.NETWORK to R.string.error_no_internet,
            TriviaError.PROTOCOL to R.string.error_network_loading_failed,
            TriviaError.PERSISTENCE to R.string.error_persistence,
            TriviaError.UNKNOWN to R.string.error_unknown
        )
        for ((error, resource) in mappings) {
            state.value = TriviaState.Failed(TriviaSdkException(error))
            runCurrent()
            assertEquals(TriviaUiState.Error(UiText.StringResource(resource)), viewModel.uiState.value)
        }
    }

    @Test
    fun `start forwards configuration and ignores duplicate pending loads`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { sdk.start(any()) } coAnswers { gate.await() }
        val config = TriviaConfig(20, 17, "easy")
        viewModel.loadQuestions(config)
        viewModel.loadQuestions(config)
        runCurrent()
        coVerify(exactly = 1) { sdk.start(config) }
        viewModel.cancelPendingLoad()
        advanceUntilIdle()
        gate.complete(Unit)
    }

    @Test
    fun `invalid amount is shown before calling SDK`() = runTest {
        runCurrent()
        viewModel.loadQuestions(1)
        assertEquals(
            TriviaUiState.Error(UiText.StringResource(R.string.error_invalid_amount)),
            viewModel.uiState.value
        )
        coVerify(exactly = 0) { sdk.start(any()) }
    }

    @Test
    fun `answers finish reset and lifecycle commands delegate to SDK`() = runTest {
        viewModel.submit("Water")
        viewModel.finishEarly()
        viewModel.saveGameOnPause()
        viewModel.onAppResumed()
        viewModel.restart()
        advanceUntilIdle()
        coVerify(exactly = 1) { sdk.submit("Water") }
        coVerify(exactly = 1) { sdk.finishEarly() }
        coVerify(exactly = 1) { sdk.pause() }
        coVerify(exactly = 1) { sdk.resume() }
        coVerify(exactly = 1) { sdk.reset() }
    }

    @Test
    fun `replay navigation is gated by SDK availability`() = runTest {
        every { sdk.canReplay } returns false
        assertFalse(viewModel.replayLastGame())
        every { sdk.canReplay } returns true
        assertTrue(viewModel.replayLastGame())
        advanceUntilIdle()
        coVerify(exactly = 1) { sdk.replay() }
    }

    @Test
    fun `categories load and language changes refresh presentation`() = runTest {
        val english = listOf(TriviaCategory(9, "General Knowledge"))
        val german = listOf(TriviaCategory(9, "Allgemeinwissen"))
        coEvery { sdk.loadCategories() } returns english
        viewModel.bootstrapApp()
        advanceUntilIdle()
        assertEquals(english, viewModel.categories.value)
        every { sdk.setContentLanguage(AppLanguage.GERMAN) } returns true
        coEvery { sdk.loadCategories() } returns german
        viewModel.onContentLanguageChanged(AppLanguage.GERMAN)
        advanceUntilIdle()
        assertEquals(german, viewModel.categories.value)
        every { sdk.setContentLanguage(AppLanguage.GERMAN) } returns false
        viewModel.onContentLanguageChanged(AppLanguage.GERMAN)
        advanceUntilIdle()
        coVerify(exactly = 2) { sdk.loadCategories() }
    }

    @Test
    fun `language before bootstrap does not start category load`() = runTest {
        every { sdk.setContentLanguage(AppLanguage.SPANISH) } returns true
        viewModel.onContentLanguageChanged(AppLanguage.SPANISH)
        advanceUntilIdle()
        coVerify(exactly = 0) { sdk.loadCategories() }
    }

    @Test
    fun `translation and persistence notices expose SDK signals`() = runTest {
        runCurrent()
        translationUnavailable.value = true
        assertTrue(viewModel.translationUnavailable.value)
        persistenceFailure.value = TriviaSdkException(TriviaError.PERSISTENCE)
        runCurrent()
        assertEquals(UiText.StringResource(R.string.error_persistence), viewModel.persistenceError.value)
        assertEquals(TriviaUiState.Idle, viewModel.uiState.value)
        persistenceFailure.value = null
        runCurrent()
        assertEquals(null, viewModel.persistenceError.value)
    }

    @Test
    fun `category failures expose retryable localized messages`() = runTest {
        coEvery { sdk.loadCategories() } throws TriviaSdkException(TriviaError.UNKNOWN)
        viewModel.loadCategories()
        advanceUntilIdle()
        assertEquals(UiText.StringResource(R.string.error_loading_categories), viewModel.categoriesError.value)
        assertFalse(viewModel.isCategoriesLoading.value)
        coEvery { sdk.loadCategories() } throws TriviaSdkException(TriviaError.NETWORK)
        viewModel.loadCategories()
        advanceUntilIdle()
        assertEquals(UiText.StringResource(R.string.error_no_internet), viewModel.categoriesError.value)
    }
}
