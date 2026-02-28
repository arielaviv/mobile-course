package com.lux.field.domain.model

enum class DpType(val label: String) {
    DP("Distribution Point"),
    JB("Junction Box"),
    MH("Manhole"),
}

data class DistributionPoint(
    val id: String,
    val label: String,
    val type: DpType,
    val latitude: Double,
    val longitude: Double,
    val photoPath: String?,
    val notes: String,
    val createdAt: Long,
    val createdBy: String,
)
