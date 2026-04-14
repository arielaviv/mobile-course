package com.field.survey.data.repository

import com.field.survey.BuildConfig
import com.field.survey.data.remote.WeatherApiService
import com.field.survey.domain.model.Weather
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository
    @Inject
    constructor(
        private val weatherApi: WeatherApiService,
    ) {

        suspend fun getWeather(
            lat: Double,
            lon: Double,
        ): Result<Weather> {
            val apiKey = BuildConfig.OPENWEATHER_API_KEY
            if (apiKey.isBlank()) return Result.failure(Exception("OpenWeather API key not configured"))

            return try {
                val response = weatherApi.getCurrentWeather(lat = lat, lon = lon, apiKey = apiKey)
                val condition = response.weather.firstOrNull()
                val main = response.main

                if (main == null) {
                    Result.failure(Exception("Invalid weather response"))
                } else {
                    Result.success(
                        Weather(
                            cityName = response.name,
                            temperature = main.temp,
                            feelsLike = main.feelsLike,
                            description = condition?.description?.replaceFirstChar { it.uppercase() } ?: "",
                            conditionCode = condition?.id ?: 0,
                            humidity = main.humidity,
                            windSpeed = response.wind?.speed ?: 0.0,
                        ),
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
