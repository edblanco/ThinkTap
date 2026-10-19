package com.dosparta.trivia.ui.viewmodel

import android.util.Log
import com.dosparta.trivia.domain.localization.IContentLocalizationRepository
import com.dosparta.trivia.domain.model.AppLanguage
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.GameSessionState
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import com.dosparta.trivia.domain.repository.ITriviaRepository
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class TriviaViewModelPersistenceTest {
    private val dispatcher = StandardTestDispatcher()
    private val sdk = mockk<TriviaSdk>()
    private lateinit var viewModel: TriviaViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { sdk.state } returns MutableStateFlow(TriviaState.Idle)
        every { sdk.persistenceFailure } returns MutableStateFlow(null)
        every { sdk.translationUnavailable } returns MutableStateFlow(false)
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
    fun `restored SDK game navigates to trivia without fetching categories`() = runTest {
        coEvery { sdk.restore() } returns true
        viewModel.bootstrapApp()
        advanceUntilIdle()
        assertEquals(StartupUiState.NavigateToTrivia, viewModel.startupState.value)
        coVerify(exactly = 0) { sdk.loadCategories() }
    }

    @Test
    fun `no restored game loads setup categories and bootstraps only once`() = runTest {
        val categories = listOf(TriviaCategory(9, "General Knowledge"))
        coEvery { sdk.loadCategories() } returns categories
        viewModel.bootstrapApp()
        viewModel.bootstrapApp()
        advanceUntilIdle()
        assertEquals(categories, viewModel.categories.value)
        assertEquals(StartupUiState.NavigateToSetup, viewModel.startupState.value)
        coVerify(exactly = 1) { sdk.restore() }
    }

    @Test
    fun `category failure is retryable at startup`() = runTest {
        coEvery { sdk.loadCategories() } throws TriviaSdkException(TriviaError.UNKNOWN)
        viewModel.bootstrapApp()
        advanceUntilIdle()
        assertEquals(
            StartupUiState.Error(UiText.StringResource(R.string.error_loading_categories)),
            viewModel.startupState.value
        )
        coEvery { sdk.loadCategories() } returns emptyList()
        viewModel.retryBootstrap()
        advanceUntilIdle()
        assertEquals(StartupUiState.NavigateToSetup, viewModel.startupState.value)
    }

    @Test
    fun `restore failure is explicit rather than silently losing saved progress`() = runTest {
        coEvery { sdk.restore() } throws TriviaSdkException(TriviaError.PERSISTENCE)
        viewModel.bootstrapApp()
        advanceUntilIdle()
        assertTrue(viewModel.startupState.value is StartupUiState.Error)
        coVerify(exactly = 0) { sdk.loadCategories() }
        coEvery { sdk.restore() } returns true
        viewModel.retryBootstrap()
        advanceUntilIdle()
        assertEquals(StartupUiState.NavigateToTrivia, viewModel.startupState.value)
    }

    @Test
    fun `authentication suspension saves without clearing and blocks lifecycle game commands`() = runTest {
        coEvery { sdk.pause() } returns Unit
        viewModel.suspendForAuthentication()
        viewModel.bootstrapApp()
        viewModel.saveGameOnPause()
        viewModel.onAppResumed()
        viewModel.submit("answer")
        viewModel.restart()
        advanceUntilIdle()
        coVerify(exactly = 1) { sdk.pause() }
        coVerify(exactly = 0) { sdk.restore() }
        coVerify(exactly = 0) { sdk.resume() }
        coVerify(exactly = 0) { sdk.submit(any()) }
        coVerify(exactly = 0) { sdk.reset() }
    }

    @Test
    fun `next authenticated entry restores the preserved game`() = runTest {
        coEvery { sdk.pause() } returns Unit
        coEvery { sdk.restore() } returns true
        viewModel.bootstrapApp()
        advanceUntilIdle()
        viewModel.suspendForAuthentication()
        viewModel.enableAfterAuthentication()
        viewModel.bootstrapApp()
        advanceUntilIdle()
        assertEquals(StartupUiState.NavigateToTrivia, viewModel.startupState.value)
        coVerify(exactly = 2) { sdk.restore() }
        coVerify(exactly = 0) { sdk.reset() }
    }

    @Test
    fun `authentication suspension exposes save failures`() = runTest {
        val failure = MutableStateFlow<TriviaSdkException?>(null)
        every { sdk.persistenceFailure } returns failure
        coEvery { sdk.pause() } answers {
            failure.value = TriviaSdkException(TriviaError.PERSISTENCE)
        }
        assertEquals(
            UiText.StringResource(R.string.error_persistence),
            viewModel.suspendForAuthentication()
        )
    }

    @Test
    fun `new sign in waits for a pending preservation write before enabling gameplay`() = runTest {
        val write = CompletableDeferred<Unit>()
        coEvery { sdk.pause() } coAnswers { write.await() }
        launch { viewModel.suspendForAuthentication() }
        runCurrent()
        launch {
            viewModel.enableAfterAuthentication()
            viewModel.bootstrapApp()
        }
        runCurrent()
        coVerify(exactly = 0) { sdk.restore() }
        write.complete(Unit)
        advanceUntilIdle()
        coVerify(exactly = 1) { sdk.restore() }
    }

    @Test
    fun `failed answer and authentication saves cannot restore stale progress on sign in`() = runTest {
        val question = TriviaQuestion(
            category = "Science",
            type = "multiple",
            difficulty = "easy",
            question = "What is H2O?",
            correctAnswer = "Water",
            options = listOf("Water", "Salt")
        )
        var stored = GameSessionState(listOf(question, question), 0, 0, 42L, emptyMap())
        val sessions = mockk<IGameSessionRepository>()
        val repository = mockk<ITriviaRepository>()
        val localization = mockk<IContentLocalizationRepository>()
        every { localization.translationUnavailable } returns MutableStateFlow(false)
        every { localization.contentLanguage } returns MutableStateFlow(AppLanguage.ENGLISH)
        coEvery { sessions.getActiveSession() } answers { stored }
        coEvery { sessions.saveGameSession(any(), any(), any(), any(), any()) } throws
            IllegalStateException("Storage unavailable")
        val controller = TriviaSdk(repository, sessions, localization)
        val vm = TriviaViewModel(controller)
        vm.bootstrapApp()
        advanceUntilIdle()
        vm.submit("Water")
        advanceUntilIdle()
        assertEquals(1, assertIs<TriviaState.Playing>(controller.state.value).session.currentIndex)
        assertEquals(0, stored.currentIndex)
        assertEquals(UiText.StringResource(R.string.error_persistence), vm.persistenceError.value)

        // Logout can preserve more than once as authentication state changes.
        repeat(2) { vm.suspendForAuthentication() }
        vm.enableAfterAuthentication()
        vm.bootstrapApp()
        advanceUntilIdle()
        assertEquals(
            StartupUiState.Error(UiText.StringResource(R.string.error_persistence)),
            vm.startupState.value
        )
        assertEquals(1, assertIs<TriviaState.Playing>(controller.state.value).session.currentIndex)
        assertEquals(1, assertIs<TriviaState.Playing>(controller.state.value).session.correctCount)
        assertEquals(0, stored.currentIndex)
        coVerify(exactly = 1) { sessions.getActiveSession() }

        coEvery { sessions.saveGameSession(any(), any(), any(), any(), any()) } answers {
            stored = GameSessionState(firstArg(), secondArg(), thirdArg(), arg(3), arg(4))
        }
        vm.retryBootstrap()
        advanceUntilIdle()
        assertEquals(StartupUiState.NavigateToTrivia, vm.startupState.value)
        assertEquals(1, assertIs<TriviaState.Playing>(controller.state.value).session.currentIndex)
        assertEquals(1, stored.currentIndex)
        assertEquals(1, stored.correctCount)
        assertEquals(mapOf(0 to "Water"), stored.selectedAnswers)
        assertEquals(null, vm.persistenceError.value)
        coVerify(exactly = 2) { sessions.getActiveSession() }
        coVerify(exactly = 0) { sessions.clearActiveSession() }
    }
}
