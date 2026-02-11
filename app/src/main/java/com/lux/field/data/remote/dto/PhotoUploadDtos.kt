package com.lux.field.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PhotoUploadResponse(
    val photoId: String,
    val url: String,
)
