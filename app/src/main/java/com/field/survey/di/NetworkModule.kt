package com.field.survey.di

import com.field.survey.BuildConfig
import com.field.survey.data.remote.AnthropicApiService
import com.field.survey.data.remote.GeocodingApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/"
    private const val ANTHROPIC_BASE_URL = "https://api.anthropic.com/"
    private const val USER_AGENT = "FieldSurvey/1.0 (arielaviv@users.noreply.github.com)"
    private const val ANTHROPIC_VERSION = "2023-06-01"

    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
            classDiscriminator = "type"
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val userAgentInterceptor =
            Interceptor { chain ->
                val request =
                    chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .build()
                chain.proceed(request)
            }

        val builder =
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(userAgentInterceptor)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideGeocodingApiService(
        okHttpClient: OkHttpClient,
        json: Json,
    ): GeocodingApiService {
        return Retrofit.Builder()
            .baseUrl(NOMINATIM_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeocodingApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAnthropicApiService(
        okHttpClient: OkHttpClient,
        json: Json,
    ): AnthropicApiService {
        val anthropicClient =
            okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request =
                        chain.request().newBuilder()
                            .header("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
                            .header("anthropic-version", ANTHROPIC_VERSION)
                            .header("content-type", "application/json")
                            .build()
                    chain.proceed(request)
                }
                .build()

        return Retrofit.Builder()
            .baseUrl(ANTHROPIC_BASE_URL)
            .client(anthropicClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AnthropicApiService::class.java)
    }
}
