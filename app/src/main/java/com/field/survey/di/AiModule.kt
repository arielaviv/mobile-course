package com.field.survey.di

import android.content.Context
import com.field.survey.data.remote.ClaudeApiService
import com.field.survey.data.remote.ElevenLabsApiService
import com.field.survey.domain.voice.AudioPlayerManager
import com.field.survey.domain.voice.SpeechRecognizerWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideClaudeApiService(): ClaudeApiService = ClaudeApiService()

    @Provides
    @Singleton
    fun provideElevenLabsApiService(): ElevenLabsApiService = ElevenLabsApiService()

    @Provides
    @Singleton
    fun provideSpeechRecognizerWrapper(
        @ApplicationContext context: Context,
    ): SpeechRecognizerWrapper = SpeechRecognizerWrapper(context)

    @Provides
    @Singleton
    fun provideAudioPlayerManager(
        @ApplicationContext context: Context,
    ): AudioPlayerManager = AudioPlayerManager(context)
}
