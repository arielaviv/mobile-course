package com.lux.field.data.remote.dto

import com.lux.field.domain.model.TaskStatus
import com.lux.field.domain.model.WorkOrderStatus
import com.lux.field.domain.model.WorkOrderType
import kotlinx.serialization.Serializable

@Serializable
data class WorkOrderDetailDto(
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
    val tasks: List<TaskDto> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class TaskDto(
    val id: String,
    val workOrderId: String,
    val sequence: Int,
    val type: String,
    val label: String,
    val description: String,
    val estimatedMinutes: Int,
    val status: TaskStatus,
    val steps: List<TaskStepDto> = emptyList(),
    val checkpointRequired: Boolean = false,
    val voiceGuidance: String? = null,
    val spliceDetail: SpliceDetailDto? = null,
    val cablePullDetail: CablePullDetailDto? = null,
)

@Serializable
data class TaskStepDto(
    val id: String,
    val sequence: Int,
    val label: String,
    val description: String,
    val isCompleted: Boolean = false,
)

@Serializable
data class SpliceDetailDto(
    val fiberCount: Int,
    val spliceType: String,
    val enclosureId: String? = null,
)

@Serializable
data class CablePullDetailDto(
    val cableType: String,
    val lengthMeters: Double,
    val startPoint: String,
    val endPoint: String,
)
