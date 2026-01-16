package com.lux.field.domain.model

data class WorkOrder(
    val id: String,
    val type: WorkOrderType,
    val baselineId: String?,
    val projectId: String,
    val tier: Int,
    val status: WorkOrderStatus,
    val priority: Int,
    val title: String,
    val description: String,
    val location: WorkOrderLocation,
    val requirements: List<String>,
    val assignment: WorkOrderAssignment?,
    val tasks: List<Task>,
    val createdAt: String,
    val updatedAt: String,
)

data class WorkOrderLocation(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String?,
)

data class WorkOrderAssignment(
    val crewId: String,
    val crewName: String,
    val assignedAt: String,
    val scheduledDate: String?,
)
