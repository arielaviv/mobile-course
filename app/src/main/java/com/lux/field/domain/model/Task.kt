package com.lux.field.domain.model

data class Task(
    val id: String,
    val workOrderId: String,
    val sequence: Int,
    val type: String,
    val label: String,
    val description: String,
    val estimatedMinutes: Int,
    val status: TaskStatus,
    val steps: List<TaskStep>,
    val checkpointRequired: Boolean,
    val voiceGuidance: String?,
    val spliceDetail: SpliceDetail?,
    val cablePullDetail: CablePullDetail?,
)

data class SpliceDetail(
    val fiberCount: Int,
    val spliceType: String,
    val enclosureId: String?,
)

data class CablePullDetail(
    val cableType: String,
    val lengthMeters: Double,
    val startPoint: String,
    val endPoint: String,
)
