package com.dosparta.trivia.ui.viewmodel

import android.util.Log
import com.dosparta.trivia.domain.model.TriviaCategory
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
}
