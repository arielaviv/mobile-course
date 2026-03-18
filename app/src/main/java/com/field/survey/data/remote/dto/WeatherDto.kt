package com.field.survey.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponseDto(
    val weather: List<WeatherConditionDto> = emptyList(),
    val main: WeatherMainDto? = null,
    val wind: WeatherWindDto? = null,
    val name: String = "",
)

@Serializable
data class WeatherConditionDto(
    val id: Int = 0,
    val main: String = "",
    val description: String = "",
    val icon: String = "",
)

@Serializable
data class WeatherMainDto(
    val temp: Double = 0.0,
    @SerialName("feels_like") val feelsLike: Double = 0.0,
    val humidity: Int = 0,
)

@Serializable
data class WeatherWindDto(
    val speed: Double = 0.0,
)
