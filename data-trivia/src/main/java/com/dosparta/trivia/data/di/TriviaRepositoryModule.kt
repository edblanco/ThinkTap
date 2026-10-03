package com.dosparta.trivia.data.di

import com.dosparta.trivia.data.repository.GameSessionRepositoryImpl
import com.dosparta.trivia.data.repository.TranslatingTriviaRepository
import com.dosparta.trivia.data.token.TriviaSessionTokenProvider
import com.dosparta.trivia.data.token.TriviaSessionTokenProviderImpl
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import com.dosparta.trivia.domain.repository.ITriviaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TriviaRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTriviaRepository(
        impl: TranslatingTriviaRepository
    ): ITriviaRepository

    @Binds
    @Singleton
    abstract fun bindGameSessionRepository(
        impl: GameSessionRepositoryImpl
    ): IGameSessionRepository

    @Binds
    @Singleton
    abstract fun bindTriviaSessionTokenProvider(
        impl: TriviaSessionTokenProviderImpl
    ): TriviaSessionTokenProvider
}
