package com.field.survey.domain.model

data class Weather(
    val cityName: String,
    val temperature: Double,
    val feelsLike: Double,
    val description: String,
    val conditionCode: Int,
    val humidity: Int,
    val windSpeed: Double,
)
