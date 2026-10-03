package com.dosparta.trivia.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TriviaUiState {
    data object Idle : TriviaUiState
    data object Loading : TriviaUiState
    data class Error(val message: UiText) : TriviaUiState
    data class Game(val session: GameSession) : TriviaUiState
    data class Result(val result: GameResult) : TriviaUiState
}

sealed interface StartupUiState {
    data object Loading : StartupUiState
    data object NavigateToSetup : StartupUiState
    data object NavigateToTrivia : StartupUiState
    data class Error(val message: UiText) : StartupUiState
}

@HiltViewModel
class TriviaViewModel @Inject constructor(private val sdk: TriviaSdk) : ViewModel() {
    private val mutableUiState = MutableStateFlow<TriviaUiState>(TriviaUiState.Idle)
    val uiState: StateFlow<TriviaUiState> = mutableUiState.asStateFlow()
    private val mutableCategories = MutableStateFlow<List<TriviaCategory>>(emptyList())
    val categories: StateFlow<List<TriviaCategory>> = mutableCategories.asStateFlow()
    private val mutableCategoriesError = MutableStateFlow<UiText?>(null)
    val categoriesError: StateFlow<UiText?> = mutableCategoriesError.asStateFlow()
    private val mutableCategoriesLoading = MutableStateFlow(false)
    val isCategoriesLoading: StateFlow<Boolean> = mutableCategoriesLoading.asStateFlow()
    private val mutableStartupState = MutableStateFlow<StartupUiState>(StartupUiState.Loading)
    val startupState: StateFlow<StartupUiState> = mutableStartupState.asStateFlow()
    private val mutablePersistenceError = MutableStateFlow<UiText?>(null)
    val persistenceError: StateFlow<UiText?> = mutablePersistenceError.asStateFlow()
    val translationUnavailable: StateFlow<Boolean> = sdk.translationUnavailable

    private var loadJob: Job? = null
    private var categoriesJob: Job? = null
    private var bootstrapped = false

    init {
        viewModelScope.launch {
            sdk.state.collect { state ->
                mutableUiState.value = when (state) {
                    TriviaState.Idle -> TriviaUiState.Idle
                    TriviaState.Loading -> TriviaUiState.Loading
                    is TriviaState.Playing -> TriviaUiState.Game(state.session)
                    is TriviaState.Finished -> TriviaUiState.Result(state.result)
                    is TriviaState.Failed -> {
                        Log.e(TAG, "Trivia command failed", state.failure)
                        TriviaUiState.Error(state.failure.toUiText())
                    }
                }
            }
        }
        viewModelScope.launch {
            sdk.persistenceFailure.collect { failure ->
                if (failure != null) Log.e(TAG, "Could not persist trivia session", failure)
                mutablePersistenceError.value = failure?.toUiText()
            }
        }
    }

    fun cancelPendingLoad() {
        loadJob?.cancel()
    }

    fun bootstrapApp() {
        if (bootstrapped) return
        bootstrapped = true
        viewModelScope.launch {
            mutableStartupState.value = StartupUiState.Loading
            try {
                if (sdk.restore()) {
                    mutableStartupState.value = StartupUiState.NavigateToTrivia
                } else {
                    loadCategoriesInternal(updateStartupState = true)
                }
            } catch (e: CancellationException) {
                bootstrapped = false
                throw e
            } catch (e: TriviaSdkException) {
                Log.e(TAG, "Trivia startup failed", e)
                mutableStartupState.value = StartupUiState.Error(e.toUiText())
                bootstrapped = false
            }
        }
    }

    fun retryBootstrap() {
        bootstrapped = false
        bootstrapApp()
    }

    fun loadCategories() {
        if (categoriesJob?.isActive == true || mutableCategoriesLoading.value) return
        categoriesJob = viewModelScope.launch { loadCategoriesInternal(updateStartupState = false) }
    }

    fun onContentLanguageChanged(language: AppLanguage) {
        if (!sdk.setContentLanguage(language)) return
        val categoriesAlreadyRequested = categories.value.isNotEmpty() || categoriesError.value != null
        if (categoriesAlreadyRequested && startupState.value !is StartupUiState.Loading) {
            val previousJob = categoriesJob
            categoriesJob = viewModelScope.launch {
                previousJob?.cancelAndJoin()
                loadCategoriesInternal(updateStartupState = false)
            }
        }
    }

    private suspend fun loadCategoriesInternal(updateStartupState: Boolean) {
        mutableCategoriesLoading.value = true
        mutableCategoriesError.value = null
        try {
            mutableCategories.value = sdk.loadCategories()
            if (updateStartupState) mutableStartupState.value = StartupUiState.NavigateToSetup
        } catch (e: CancellationException) {
            if (updateStartupState) bootstrapped = false
            throw e
        } catch (e: TriviaSdkException) {
            Log.e(TAG, "Could not load trivia categories", e)
            val message = if (e.error == TriviaError.NETWORK) e.toUiText()
                else UiText.StringResource(R.string.error_loading_categories)
            mutableCategoriesError.value = message
            if (updateStartupState) {
                mutableStartupState.value = StartupUiState.Error(message)
                bootstrapped = false
            }
        } finally {
            mutableCategoriesLoading.value = false
        }
    }

    fun loadQuestions(amount: Int = TriviaConfig.DEFAULT_AMOUNT) {
        val config = try {
            TriviaConfig(amount = amount)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid trivia configuration", e)
            mutableUiState.value = TriviaUiState.Error(UiText.StringResource(R.string.error_invalid_amount))
            return
        }
        loadQuestions(config)
    }

    fun loadQuestions(config: TriviaConfig) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch { sdk.start(config) }
    }

    fun submit(answer: String) {
        viewModelScope.launch { sdk.submit(answer) }
    }

    fun finishEarly() {
        viewModelScope.launch { sdk.finishEarly() }
    }

    fun saveGameOnPause() {
        viewModelScope.launch { sdk.pause() }
    }

    fun onAppResumed() {
        viewModelScope.launch { sdk.resume() }
    }

    fun restart() {
        cancelPendingLoad()
        viewModelScope.launch { sdk.reset() }
        if (categories.value.isEmpty()) loadCategories()
    }

    fun replayLastGame(): Boolean {
        if (!sdk.canReplay) return false
        cancelPendingLoad()
        viewModelScope.launch { sdk.replay() }
        return true
    }

    private fun TriviaSdkException.toUiText(): UiText = UiText.StringResource(
        when (error) {
            TriviaError.INVALID_CONFIGURATION -> R.string.error_invalid_amount
            TriviaError.NO_QUESTIONS -> R.string.error_no_questions_available
            TriviaError.NO_RESULTS -> R.string.error_no_results_for_filters
            TriviaError.INVALID_QUERY -> R.string.error_invalid_query_parameters
            TriviaError.RATE_LIMIT -> R.string.error_rate_limit
            TriviaError.NETWORK -> R.string.error_no_internet
            TriviaError.PROTOCOL -> R.string.error_network_loading_failed
            TriviaError.PERSISTENCE -> R.string.error_persistence
            TriviaError.UNKNOWN -> R.string.error_unknown
        }
    )

    private companion object {
        const val TAG = "TriviaViewModel"
    }
}
