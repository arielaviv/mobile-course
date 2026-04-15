package com.field.survey.data.repository

import com.field.survey.data.remote.GeocodingApiService
import com.field.survey.data.remote.dto.NominatimAddressDto
import com.field.survey.domain.model.LocationAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeocodingRepository
    @Inject
    constructor(
        private val api: GeocodingApiService,
    ) {

        suspend fun reverseGeocode(
            lat: Double,
            lon: Double,
        ): Result<LocationAddress> {
            return try {
                val response = api.reverseGeocode(lat = lat, lon = lon)
                val primary =
                    response.address?.primaryLine()?.takeIf { it.isNotBlank() }
                        ?: response.displayName.substringBefore(',').trim()
                val secondary =
                    response.address?.secondaryLine()?.takeIf { it.isNotBlank() }
                        ?: response.displayName.substringAfter(',', "").substringBefore(',').trim()
                Result.success(LocationAddress(primary = primary, secondary = secondary))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        private fun NominatimAddressDto.primaryLine(): String =
            road ?: pedestrian ?: footway ?: neighbourhood ?: suburb ?: quarter ?: ""

        private fun NominatimAddressDto.secondaryLine(): String =
            city ?: town ?: village ?: municipality ?: county ?: state ?: ""
    }
