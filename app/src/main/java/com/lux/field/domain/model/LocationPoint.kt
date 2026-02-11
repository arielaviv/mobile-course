package com.lux.field.domain.model

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float,
    val bearing: Float,
    val altitude: Double,
    val timestamp: Long,
)
