package com.dosparta.triviagame2

import android.app.Application
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dosparta.core.ui.theme.TriviaGame2Theme
import com.dosparta.trivia.domain.localization.IContentLocalizationRepository
import com.dosparta.trivia.domain.model.AppLanguage
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.GameSessionState
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import com.dosparta.trivia.domain.repository.ITriviaRepository
import com.dosparta.trivia.sdk.TriviaSdk
import com.dosparta.trivia.ui.navigation.TriviaNavHost
import com.dosparta.trivia.ui.viewmodel.TriviaViewModel
import com.dosparta.triviagame2.auth.AuthState
import com.dosparta.triviagame2.auth.AuthException
import com.dosparta.triviagame2.auth.AuthError
import com.dosparta.triviagame2.auth.AuthViewModel
import com.dosparta.triviagame2.auth.FakeAuthRepository
import com.dosparta.triviagame2.reminder.ReminderPreferencesStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU], application = Application::class)
class ProAppEntryTest {
    @get:Rule
    val compose = createComposeRule()
    private val authRepository = FakeAuthRepository()
    private val sessions = MemorySessions()
    private var restores = 0
    private var categoryLoads = 0

    @Test
    fun `signed out entry hides persisted trivia until login`() {
        val vm = TriviaViewModel(createSdk())
        show(vm)
        compose.onNodeWithTag("pro_login").assertIsDisplayed()
        compose.onNodeWithTag("trivia_screen").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, restores); assertEquals(0, categoryLoads) }
        compose.runOnIdle { authRepository.state.value = AuthState.SignedIn("first", "Player") }
        compose.onNodeWithText("First question?").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, restores) }
    }

    @Test
    fun `logout preserves progress and another account restores it after controller recreation`() {
        val context = RuntimeEnvironment.getApplication()
        val settings = ReminderPreferencesStore(context)
        settings.saveEnabled(true)
        settings.saveTime(8, 25)
        val before = settings.load()
        val vm = mutableStateOf(TriviaViewModel(createSdk()))
        authRepository.state.value = AuthState.SignedIn("first", "First player")
        val auth = AuthViewModel(authRepository)
        compose.setContent {
            TriviaGame2Theme {
                ProAppEntry(vm.value, auth) {
                    TriviaNavHost(viewModel = vm.value, selectedLanguage = AppLanguage.GERMAN)
                }
            }
        }
        compose.onNodeWithText("First question?").assertIsDisplayed()
        compose.runOnIdle { vm.value.submit("A") }
        compose.onNodeWithText("Second question?").assertIsDisplayed()
        compose.onNodeWithTag("pro_sign_out").performClick()
        compose.onNodeWithTag("pro_login").assertIsDisplayed()
        compose.onNodeWithTag("trivia_screen").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, sessions.saved?.currentIndex)
            assertEquals(0, sessions.clearCalls)
            assertEquals(before, settings.load())
            vm.value = TriviaViewModel(createSdk())
            authRepository.state.value = AuthState.SignedIn("second", "Second player")
        }
        compose.onNodeWithText("Second question?").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(0, sessions.clearCalls)
            assertEquals(before, settings.load())
        }
    }

    @Test
    fun `provider invalidation hides trivia without deleting the game`() {
        authRepository.state.value = AuthState.SignedIn("first", null)
        show(TriviaViewModel(createSdk()))
        compose.onNodeWithText("First question?").assertIsDisplayed()
        compose.runOnIdle { authRepository.state.value = AuthState.SignedOut }
        compose.onNodeWithTag("pro_login").assertIsDisplayed()
        compose.onNodeWithTag("trivia_screen").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, sessions.clearCalls) }
    }

    @Test
    fun `missing Firebase configuration is explicit and cannot bypass login`() {
        authRepository.state.value = AuthState.ConfigurationError
        show(TriviaViewModel(createSdk()))
        compose.onNodeWithTag("google_sign_in").assertIsNotEnabled()
        compose.onNodeWithText(
            RuntimeEnvironment.getApplication().getString(R.string.auth_error_configuration)
        ).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, restores) }
    }

    @Test
    fun `account change preserves progress and reboots authenticated navigation`() {
        authRepository.state.value = AuthState.SignedIn("first", null)
        val vm = TriviaViewModel(createSdk())
        show(vm)
        compose.onNodeWithText("First question?").assertIsDisplayed()
        compose.runOnIdle { vm.submit("A") }
        compose.onNodeWithText("Second question?").assertIsDisplayed()
        compose.runOnIdle { authRepository.state.value = AuthState.SignedIn("second", null) }
        compose.onNodeWithText("Second question?").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(2, restores)
            assertEquals(0, sessions.clearCalls)
            assertEquals(1, sessions.saved?.currentIndex)
        }
    }

    @Test
    fun `persisted sign in can enter a saved game when provider refresh is offline`() {
        authRepository.state.value = AuthState.SignedIn("offline", null)
        authRepository.onRefresh = { throw AuthException(AuthError.NETWORK) }
        show(TriviaViewModel(createSdk()))
        compose.onNodeWithText("First question?").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(AuthState.SignedIn("offline", null), authRepository.state.value)
            assertEquals(0, authRepository.signInCalls)
        }
    }

    private fun show(vm: TriviaViewModel) {
        val auth = AuthViewModel(authRepository)
        compose.setContent {
            TriviaGame2Theme {
                ProAppEntry(vm, auth) { TriviaNavHost(viewModel = vm) }
            }
        }
    }

    private fun createSdk(): TriviaSdk {
        val repository = object : ITriviaRepository {
            override suspend fun getQuestions(config: TriviaConfig) = questions()
            override suspend fun getCategories(): List<TriviaCategory> {
                categoryLoads++
                return emptyList()
            }
        }
        val localization = object : IContentLocalizationRepository {
            override val contentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
            override val translationUnavailable = MutableStateFlow(false)
            override fun setContentLanguage(language: AppLanguage) { contentLanguage.value = language }
            override fun setTranslationUnavailable(unavailable: Boolean) {
                translationUnavailable.value = unavailable
            }
        }
        val trackingSessions = object : IGameSessionRepository by sessions {
            override suspend fun getActiveSession(): GameSessionState? {
                restores++
                return sessions.saved
            }
        }
        return TriviaSdk(repository, trackingSessions, localization)
    }
}

private class MemorySessions : IGameSessionRepository {
    var saved: GameSessionState? = GameSessionState(questions(), 0, 0, 42L, emptyMap())
    var clearCalls = 0
    override suspend fun saveGameSession(
        questions: List<TriviaQuestion>,
        currentIndex: Int,
        correctCount: Int,
        activeElapsedMillis: Long,
        selectedAnswers: Map<Int, String>
    ) {
        saved = GameSessionState(questions, currentIndex, correctCount, activeElapsedMillis, selectedAnswers)
    }
    override fun getActiveSessionFlow() = flowOf(saved)
    override suspend fun getActiveSession() = saved
    override suspend fun clearActiveSession() { clearCalls++; saved = null }
}

private fun questions() = listOf("First question?", "Second question?").map {
    TriviaQuestion("Science", "multiple", "easy", it, "A", listOf("A", "B", "C", "D"))
}
