package com.dosparta.trivia.data.di

import android.content.Context
import androidx.room.Room
import com.dosparta.trivia.data.local.database.TriviaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideTriviaDatabase(
        @ApplicationContext context: Context
    ): TriviaDatabase {
        return Room.databaseBuilder(
            context,
            TriviaDatabase::class.java,
            "trivia_database"
        ).build()
    }

    @Singleton
    @Provides
    fun provideGameSessionDao(
        database: TriviaDatabase
    ) = database.gameSessionDao()
}
