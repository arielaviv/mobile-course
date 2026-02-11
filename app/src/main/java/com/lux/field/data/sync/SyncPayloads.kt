package com.lux.field.data.sync

import kotlinx.serialization.Serializable

@Serializable
data class TaskUpdatePayload(
    val workOrderId: String,
    val taskId: String,
    val status: String,
    val notes: String? = null,
    val timestamp: String,
)

@Serializable
data class PhotoUploadPayload(
    val photoId: String,
    val taskId: String,
    val workOrderId: String,
    val filePath: String,
    val cameraFacing: String,
    val capturedAt: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
