package com.dosparta.trivia.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.domain.game.GameSession
import com.dosparta.trivia.domain.game.StartupDecision
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.usecase.ClearGameSessionUseCase
import com.dosparta.trivia.domain.usecase.FinishGameUseCase
import com.dosparta.trivia.domain.usecase.LoadCategoriesUseCase
import com.dosparta.trivia.domain.usecase.PersistGameSessionUseCase
import com.dosparta.trivia.domain.usecase.ResolveAppStartupUseCase
import com.dosparta.trivia.domain.usecase.StartGameSession
import com.dosparta.trivia.domain.usecase.SubmitAnswerUseCase
import com.dosparta.trivia.ui.R
import com.dosparta.trivia.ui.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
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

/**
 * Holds and drives the UI state for a trivia session.
 */
@HiltViewModel
class TriviaViewModel @Inject constructor(
    private val startGame: StartGameSession,
    private val submitAnswer: SubmitAnswerUseCase,
    private val finishGame: FinishGameUseCase,
    private val loadCategoriesUseCase: LoadCategoriesUseCase,
    private val resolveStartupUseCase: ResolveAppStartupUseCase,
    private val persistGameSessionUseCase: PersistGameSessionUseCase,
    private val clearGameSessionUseCase: ClearGameSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TriviaUiState>(TriviaUiState.Idle)
    val uiState: StateFlow<TriviaUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<TriviaCategory>>(emptyList())
    val categories: StateFlow<List<TriviaCategory>> = _categories.asStateFlow()

    private val _categoriesError = MutableStateFlow<UiText?>(null)
    val categoriesError: StateFlow<UiText?> = _categoriesError.asStateFlow()

    private val _isCategoriesLoading = MutableStateFlow(false)
    val isCategoriesLoading: StateFlow<Boolean> = _isCategoriesLoading.asStateFlow()

    private val _startupState = MutableStateFlow<StartupUiState>(StartupUiState.Loading)
    val startupState: StateFlow<StartupUiState> = _startupState.asStateFlow()

    private var loadJob: Job? = null
    private var bootstrapped = false
    private var pausedElapsedMillis: Long? = null
    private var replayQuestions: List<TriviaQuestion> = emptyList()

    fun cancelPendingLoad() {
        loadJob?.cancel()
    }

    fun bootstrapApp() {
        if (bootstrapped) return
        bootstrapped = true

        viewModelScope.launch {
            _startupState.value = StartupUiState.Loading

            val startupDecision = try {
                resolveStartupUseCase()
            } catch (e: CancellationException) {
                bootstrapped = false
                return@launch
            } catch (e: Exception) {
                StartupDecision.LoadCategories
            }

            when (startupDecision) {
                is StartupDecision.RestoreGame -> {
                    val activeElapsedMillis = normalizePersistedElapsedMillis(startupDecision.session.activeElapsedMillis)
                    replayQuestions = startupDecision.session.questions
                    _uiState.value = TriviaUiState.Game(
                        GameSession(
                            questions = startupDecision.session.questions,
                            currentIndex = startupDecision.session.currentIndex,
                            correctCount = startupDecision.session.correctCount,
                            startTimeMillis = System.currentTimeMillis() - activeElapsedMillis
                        )
                    )
                    _startupState.value = StartupUiState.NavigateToTrivia
                }
                StartupDecision.LoadCategories -> loadCategoriesInternal(updateStartupState = true)
            }
        }
    }

    fun retryBootstrap() {
        bootstrapped = false
        bootstrapApp()
    }

    fun loadCategories() {
        if (_isCategoriesLoading.value) return

        viewModelScope.launch {
            loadCategoriesInternal(updateStartupState = false)
        }
    }

    private suspend fun loadCategoriesInternal(updateStartupState: Boolean) {
        _isCategoriesLoading.value = true
        _categoriesError.value = null
        try {
            _categories.value = loadCategoriesUseCase()
            if (updateStartupState) {
                _startupState.value = StartupUiState.NavigateToSetup
            }
        } catch (e: CancellationException) {
            if (updateStartupState) {
                bootstrapped = false
            }
            return
        } catch (e: IOException) {
            val message = UiText.StringResource(R.string.error_no_internet)
            _categoriesError.value = message
            if (updateStartupState) {
                _startupState.value = StartupUiState.Error(message)
                bootstrapped = false
            }
        } catch (e: Exception) {
            val message = UiText.StringResource(R.string.error_loading_categories)
            _categoriesError.value = message
            if (updateStartupState) {
                _startupState.value = StartupUiState.Error(message)
                bootstrapped = false
            }
        } finally {
            _isCategoriesLoading.value = false
        }
    }

    /** Kick off a new game with [amount] questions. */
    fun loadQuestions(amount: Int = 10) {
        val config = try {
            TriviaConfig(amount = amount)
        } catch (e: IllegalArgumentException) {
            _uiState.value = TriviaUiState.Error(UiText.StringResource(R.string.error_invalid_amount))
            return
        }
        loadQuestions(config)
    }

    fun loadQuestions(config: TriviaConfig) {
        if (_uiState.value is TriviaUiState.Loading) return

        cancelPendingLoad()
        loadJob = viewModelScope.launch {
            _uiState.value = TriviaUiState.Loading
            try {
                val session = startGame(config)
                if (!coroutineContext.isActive) return@launch
                pausedElapsedMillis = null
                replayQuestions = session.questions
                _uiState.value = TriviaUiState.Game(session)
                // Save the newly started game session
                saveGameSession(session)
            } catch (e: CancellationException) {
                return@launch
            } catch (e: IOException) {
                _uiState.value = TriviaUiState.Error(UiText.StringResource(R.string.error_no_internet))
            } catch (e: IllegalArgumentException) {
                _uiState.value = TriviaUiState.Error(UiText.StringResource(R.string.error_invalid_amount))
            } catch (e: Exception) {
                _uiState.value = TriviaUiState.Error(
                    if (e.message?.contains("rate limit", ignoreCase = true) == true) {
                        UiText.StringResource(R.string.error_rate_limit)
                    } else if (e.message?.contains("no results", ignoreCase = true) == true) {
                        UiText.StringResource(R.string.error_no_results_for_filters)
                    } else if (e.message?.contains("rejected query", ignoreCase = true) == true) {
                        UiText.StringResource(R.string.error_invalid_query_parameters)
                    } else if (e.message?.contains("HTTP", ignoreCase = true) == true || e.message?.contains("response", ignoreCase = true) == true) {
                        UiText.StringResource(R.string.error_network_loading_failed)
                    } else {
                        UiText.StringResource(R.string.error_unknown)
                    }
                )
            }
        }
    }

    /** Submit an [answer] and advance the session (or finish if it was the last question). */
    fun submit(answer: String) {
        val current = (uiState.value as? TriviaUiState.Game)?.session ?: run {
            if (uiState.value is TriviaUiState.Result) return
            _uiState.value = TriviaUiState.Error(UiText.StringResource(R.string.error_no_questions_available))
            return
        }

        if (current.currentIndex >= current.questions.size) {
            finish(current)
            return
        }
        if (current.currentQuestion == null) {
            _uiState.value = TriviaUiState.Error(UiText.StringResource(R.string.error_no_questions_available))
            return
        }

        val updated = submitAnswer(current, answer)
        if (updated.currentIndex >= updated.questions.size) {
            pausedElapsedMillis = null
            replayQuestions = updated.questions
            _uiState.value = TriviaUiState.Result(finishGame(updated))
            // Clear persisted session when game finishes
            clearGameSessionAsync()
        } else {
            _uiState.value = TriviaUiState.Game(updated)
            // Save the updated game session
            saveGameSession(updated)
        }
    }

    /** Finalize the game and emit a [GameResult]. */
    private fun finish(session: GameSession) {
        _uiState.value = TriviaUiState.Result(finishGame(session))
    }

    /** Save the current game session to persistent storage. */
    private fun saveGameSession(session: GameSession, activeElapsedMillisOverride: Long? = null) {
        viewModelScope.launch {
            try {
                val activeElapsedMillis = activeElapsedMillisOverride ?: calculateActiveElapsedMillis(session)
                persistGameSessionUseCase(
                    questions = session.questions,
                    currentIndex = session.currentIndex,
                    correctCount = session.correctCount,
                    activeElapsedMillis = activeElapsedMillis,
                    selectedAnswers = emptyMap()
                )
            } catch (e: Exception) {
                // Silently fail persistence - game still progresses normally
            }
        }
    }

    /** Save the current game when the app goes to background (onPause). */
    fun saveGameOnPause() {
        val currentState = _uiState.value
        if (currentState is TriviaUiState.Game) {
            val activeElapsedMillis = calculateActiveElapsedMillis(currentState.session)
            pausedElapsedMillis = activeElapsedMillis
            saveGameSession(currentState.session, activeElapsedMillisOverride = activeElapsedMillis)
        }
    }

    fun onAppResumed() {
        val elapsed = pausedElapsedMillis ?: return
        val currentState = _uiState.value as? TriviaUiState.Game ?: return
        val rebasedSession = currentState.session.copy(
            startTimeMillis = System.currentTimeMillis() - elapsed
        )
        _uiState.value = TriviaUiState.Game(rebasedSession)
        pausedElapsedMillis = null
    }

    /** Clear the persisted game session asynchronously. */
    private fun clearGameSessionAsync() {
        viewModelScope.launch {
            try {
                clearGameSessionUseCase()
            } catch (e: Exception) {
                // Silently fail - doesn't affect UI
            }
        }
    }

    /** Reset to the configuration screen without immediately launching a new game. */
    fun restart() {
        cancelPendingLoad()
        pausedElapsedMillis = null
        viewModelScope.launch {
            clearGameSessionUseCase()
        }
        if (_categories.value.isEmpty()) {
            loadCategories()
        }
        _uiState.value = TriviaUiState.Idle
    }

    fun replayLastGame(): Boolean {
        if (replayQuestions.isEmpty()) return false

        cancelPendingLoad()
        pausedElapsedMillis = null
        val session = GameSession(questions = replayQuestions)
        _uiState.value = TriviaUiState.Game(session)
        saveGameSession(session)
        return true
    }

    override fun onCleared() {
        super.onCleared()
        cancelPendingLoad()
    }

    private fun calculateActiveElapsedMillis(session: GameSession): Long {
        val elapsed = System.currentTimeMillis() - session.startTimeMillis
        return if (elapsed < 0L) 0L else elapsed
    }

    private fun normalizePersistedElapsedMillis(rawMillis: Long): Long {
        val thirtyDaysMillis = 30L * 24L * 60L * 60L * 1000L
        return if (rawMillis > thirtyDaysMillis) {
            val legacyElapsed = System.currentTimeMillis() - rawMillis
            if (legacyElapsed < 0L) 0L else legacyElapsed
        } else {
            if (rawMillis < 0L) 0L else rawMillis
        }
    }
}
