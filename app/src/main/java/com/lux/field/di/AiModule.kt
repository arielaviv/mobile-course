package com.lux.field.di

import com.lux.field.data.remote.ClaudeApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideClaudeApiService(): ClaudeApiService = ClaudeApiService()
}
