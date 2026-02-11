package com.lux.field.di

import android.content.Context
import com.lux.field.BuildConfig
import com.mapbox.navigation.tripdata.progress.api.MapboxTripProgressApi
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.voice.api.MapboxSpeechApi
import com.mapbox.navigation.voice.api.MapboxVoiceInstructionsPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {

    @Provides
    @Singleton
    fun provideTripProgressApi(@ApplicationContext context: Context): MapboxTripProgressApi {
        return MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(context).build()
        )
    }

    @Provides
    @Singleton
    fun provideSpeechApi(@ApplicationContext context: Context): MapboxSpeechApi {
        return MapboxSpeechApi(context, BuildConfig.MAPBOX_PUBLIC_TOKEN, "en")
    }

    @Provides
    @Singleton
    fun provideVoiceInstructionsPlayer(@ApplicationContext context: Context): MapboxVoiceInstructionsPlayer {
        return MapboxVoiceInstructionsPlayer(context, BuildConfig.MAPBOX_PUBLIC_TOKEN, "en")
    }
}
