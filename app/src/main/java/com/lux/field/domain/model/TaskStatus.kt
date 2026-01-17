package com.lux.field.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TaskStatus {
    @SerialName("pending") PENDING,
    @SerialName("in_progress") IN_PROGRESS,
    @SerialName("blocked") BLOCKED,
    @SerialName("escalated") ESCALATED,
    @SerialName("coa_pending") COA_PENDING,
    @SerialName("coa_approved") COA_APPROVED,
    @SerialName("coa_rejected") COA_REJECTED,
    @SerialName("completed") COMPLETED,
    @SerialName("cancelled") CANCELLED,
    @SerialName("skipped") SKIPPED,
}
