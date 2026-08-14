package com.dosparta.trivia.ui


import com.dosparta.trivia.data.di.TriviaRepositoryModule
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.ITriviaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * In tests, we want to inject a FakeTriviaRepository,
 * but still use the real domain use-cases.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [TriviaRepositoryModule::class]
)
object TestModule {

    @Provides
    @Singleton
    fun provideTriviaRepository(): ITriviaRepository = FakeTriviaRepository(
        questions = listOf(
            TriviaQuestion(
                "General", "boolean", "easy",
                "Is sky blue?", "True", listOf("True", "False")
            ),
            TriviaQuestion(
                "Science", "multiple", "medium",
                "2+2=?", "4", listOf("4", "3", "5", "2")
            )
        )
    )

    // No need to provide StartGameSession, SubmitAnswerUseCase, or FinishGameUseCase:
    // Hilt will see their @Inject constructors and supply them automatically,
    // wiring in our FakeTriviaRepository.
}
