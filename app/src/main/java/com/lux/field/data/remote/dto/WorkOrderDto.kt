package com.lux.field.data.remote.dto

import com.lux.field.domain.model.WorkOrderStatus
import com.lux.field.domain.model.WorkOrderType
import kotlinx.serialization.Serializable

@Serializable
data class WorkOrderDto(
    val id: String,
    val type: WorkOrderType,
    val baselineId: String? = null,
    val projectId: String,
    val tier: Int,
    val status: WorkOrderStatus,
    val priority: Int,
    val title: String,
    val description: String,
    val location: LocationDto,
    val requirements: List<String> = emptyList(),
    val assignment: AssignmentDto? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class LocationDto(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String? = null,
)

@Serializable
data class AssignmentDto(
    val crewId: String,
    val crewName: String,
    val assignedAt: String,
    val scheduledDate: String? = null,
)
