package com.field.survey.data.remote.dto

import com.field.survey.domain.model.TaskStatus
import kotlinx.serialization.Serializable

@Serializable
data class TaskUpdateRequest(
    val workOrderId: String,
    val taskId: String,
    val status: TaskStatus,
    val notes: String? = null,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val timestamp: String,
)

@Serializable
data class TaskUpdateResponse(
    val success: Boolean,
    val taskId: String,
    val newStatus: TaskStatus,
)
