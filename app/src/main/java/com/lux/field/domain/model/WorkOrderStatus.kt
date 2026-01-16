package com.lux.field.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WorkOrderStatus {
    @SerialName("draft") DRAFT,
    @SerialName("pending") PENDING,
    @SerialName("scheduled") SCHEDULED,
    @SerialName("in_progress") IN_PROGRESS,
    @SerialName("completed") COMPLETED,
    @SerialName("failed") FAILED,
    @SerialName("cancelled") CANCELLED,
}
