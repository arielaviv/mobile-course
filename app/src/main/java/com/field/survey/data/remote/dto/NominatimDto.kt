package com.field.survey.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NominatimReverseDto(
    @SerialName("display_name") val displayName: String = "",
    val address: NominatimAddressDto? = null,
)

@Serializable
data class NominatimAddressDto(
    val road: String? = null,
    val pedestrian: String? = null,
    val footway: String? = null,
    val neighbourhood: String? = null,
    val suburb: String? = null,
    val quarter: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val municipality: String? = null,
    val county: String? = null,
    val state: String? = null,
    val country: String? = null,
)
