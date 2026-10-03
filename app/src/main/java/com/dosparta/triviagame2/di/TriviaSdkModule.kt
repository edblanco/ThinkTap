package com.dosparta.triviagame2.di

import android.content.Context
import com.dosparta.trivia.sdk.TriviaSdk
import com.dosparta.trivia.sdk.android.AndroidTriviaSdk
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object TriviaSdkModule {
    @Provides
    @ViewModelScoped
    fun provideTriviaSdk(@ApplicationContext context: Context): TriviaSdk =
        AndroidTriviaSdk.create(context)
}
