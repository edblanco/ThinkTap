package com.dosparta.trivia.data.di

import com.dosparta.trivia.data.remote.api.TriviaApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TriviaDataModule {
    @Provides
    @Singleton
    fun provideTriviaApi(retrofit: Retrofit): TriviaApi =
        retrofit.create(TriviaApi::class.java)
}
