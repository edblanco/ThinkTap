package com.dosparta.trivia.data.di

import com.dosparta.trivia.data.localization.ContentLocalizationRepositoryImpl
import com.dosparta.trivia.data.localization.MlKitTextTranslator
import com.dosparta.trivia.domain.localization.IContentLocalizationRepository
import com.dosparta.trivia.domain.localization.ITextTranslator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalizationModule {

    @Binds
    @Singleton
    abstract fun bindContentLocalizationRepository(
        impl: ContentLocalizationRepositoryImpl
    ): IContentLocalizationRepository

    @Binds
    @Singleton
    abstract fun bindTextTranslator(impl: MlKitTextTranslator): ITextTranslator
}
