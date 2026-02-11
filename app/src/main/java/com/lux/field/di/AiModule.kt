package com.lux.field.di

import android.content.Context
import com.lux.field.data.remote.ClaudeApiService
import com.lux.field.data.remote.ElevenLabsApiService
import com.lux.field.domain.voice.AudioPlayerManager
import com.lux.field.domain.voice.SpeechRecognizerWrapper
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
