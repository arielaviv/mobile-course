package com.lux.field.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocationBatchRequest(
    val crewId: String,
    val userId: String,
    val points: List<LocationPointDto>,
)

@Serializable
data class LocationPointDto(
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val speed: Float,
    val bearing: Float,
    val altitude: Double,
    val ts: Long,
)

@Serializable
data class LocationBatchResponse(
    val accepted: Int,
)
