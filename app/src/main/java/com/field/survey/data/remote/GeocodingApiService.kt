package com.field.survey.data.remote

import com.field.survey.data.remote.dto.NominatimReverseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApiService {

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json",
        @Query("zoom") zoom: Int = 18,
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("accept-language") language: String = "en",
    ): NominatimReverseDto
}
