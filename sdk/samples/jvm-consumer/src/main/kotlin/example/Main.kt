package example

import com.dosparta.trivia.domain.localization.IContentLocalizationRepository
import com.dosparta.trivia.domain.model.AppLanguage
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.GameSessionState
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import com.dosparta.trivia.domain.repository.ITriviaRepository
import com.dosparta.trivia.sdk.TriviaSdk
import com.dosparta.trivia.sdk.TriviaState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val question = TriviaQuestion(
        category = "Science", type = "multiple", difficulty = "easy",
        question = "What is H2O?", correctAnswer = "Water", options = listOf("Water", "Salt")
    )
    val repository = object : ITriviaRepository {
        override suspend fun getQuestions(config: TriviaConfig) = listOf(question)
        override suspend fun getCategories() = listOf(TriviaCategory(17, "Science"))
    }
    val saved = MutableStateFlow<GameSessionState?>(null)
    val sessions = object : IGameSessionRepository {
        override fun getActiveSessionFlow() = saved
        override suspend fun getActiveSession() = saved.value
        override suspend fun clearActiveSession() { saved.value = null }
        override suspend fun saveGameSession(
            questions: List<TriviaQuestion>, currentIndex: Int, correctCount: Int,
            activeElapsedMillis: Long, selectedAnswers: Map<Int, String>
        ) {
            saved.value = GameSessionState(
                questions, currentIndex, correctCount, activeElapsedMillis, selectedAnswers
            )
        }
    }
    val localization = object : IContentLocalizationRepository {
        override val contentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
        override val translationUnavailable = MutableStateFlow(false)
        override fun setContentLanguage(language: AppLanguage) { contentLanguage.value = language }
        override fun setTranslationUnavailable(unavailable: Boolean) { translationUnavailable.value = unavailable }
    }
    val sdk = TriviaSdk(repository, sessions, localization)
    check(!sdk.restore())
    sdk.start()
    check(saved.value != null)
    sdk.submit("Water")
    val finished = sdk.state.value as TriviaState.Finished
    check(finished.result.correctAnswers == 1)
    check(saved.value == null)
    check(sdk.persistenceFailure.value == null)
    println("Published SDK consumer passed: ${finished.result.correctAnswers}/${finished.result.totalQuestions}")
}
