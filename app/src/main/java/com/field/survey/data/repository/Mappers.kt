package com.field.survey.data.repository

import com.field.survey.data.local.entity.ChatMessageEntity
import com.field.survey.data.local.entity.TaskEntity
import com.field.survey.data.local.entity.TaskPhotoEntity
import com.field.survey.data.local.entity.WorkOrderEntity
import com.field.survey.data.remote.dto.WorkOrderDetailDto
import com.field.survey.data.remote.dto.WorkOrderDto
import com.field.survey.domain.model.CablePullDetail
import com.field.survey.domain.model.CameraFacing
import com.field.survey.domain.model.ChatMessage
import com.field.survey.domain.model.ChatRole
import com.field.survey.domain.model.PhotoAnalysisStatus
import com.field.survey.domain.model.SpliceDetail
import com.field.survey.domain.model.Task
import com.field.survey.domain.model.TaskPhoto
import com.field.survey.domain.model.TaskStatus
import com.field.survey.domain.model.TaskStep
import com.field.survey.domain.model.WorkOrder
import com.field.survey.domain.model.WorkOrderAssignment
import com.field.survey.domain.model.WorkOrderLocation
import com.field.survey.domain.model.WorkOrderStatus
import com.field.survey.domain.model.WorkOrderType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

// --- WorkOrderDto -> Domain ---

fun WorkOrderDto.toDomain(): WorkOrder = WorkOrder(
    id = id,
    type = type,
    baselineId = baselineId,
    projectId = projectId,
    tier = tier,
    status = status,
    priority = priority,
    title = title,
    description = description,
    location = WorkOrderLocation(
        address = location.address,
        latitude = location.latitude,
        longitude = location.longitude,
        zoneId = location.zoneId,
    ),
    requirements = requirements,
    assignment = assignment?.let {
        WorkOrderAssignment(
            crewId = it.crewId,
            crewName = it.crewName,
            assignedAt = it.assignedAt,
            scheduledDate = it.scheduledDate,
        )
    },
    tasks = emptyList(),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun WorkOrderDetailDto.toDomain(): WorkOrder = WorkOrder(
    id = id,
    type = type,
    baselineId = baselineId,
    projectId = projectId,
    tier = tier,
    status = status,
    priority = priority,
    title = title,
    description = description,
    location = WorkOrderLocation(
        address = location.address,
        latitude = location.latitude,
        longitude = location.longitude,
        zoneId = location.zoneId,
    ),
    requirements = requirements,
    assignment = assignment?.let {
        WorkOrderAssignment(
            crewId = it.crewId,
            crewName = it.crewName,
            assignedAt = it.assignedAt,
            scheduledDate = it.scheduledDate,
        )
    },
    tasks = tasks.map { dto ->
        Task(
            id = dto.id,
            workOrderId = dto.workOrderId,
            sequence = dto.sequence,
            type = dto.type,
            label = dto.label,
            description = dto.description,
            estimatedMinutes = dto.estimatedMinutes,
            status = dto.status,
            steps = dto.steps.map { step ->
                TaskStep(
                    id = step.id,
                    sequence = step.sequence,
                    label = step.label,
                    description = step.description,
                    isCompleted = step.isCompleted,
                )
            },
            checkpointRequired = dto.checkpointRequired,
            voiceGuidance = dto.voiceGuidance,
            spliceDetail = dto.spliceDetail?.let {
                SpliceDetail(it.fiberCount, it.spliceType, it.enclosureId)
            },
            cablePullDetail = dto.cablePullDetail?.let {
                CablePullDetail(it.cableType, it.lengthMeters, it.startPoint, it.endPoint)
            },
        )
    },
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// --- Domain -> Entity ---

fun WorkOrder.toEntity(): WorkOrderEntity = WorkOrderEntity(
    id = id,
    type = type.name.lowercase(),
    baselineId = baselineId,
    projectId = projectId,
    tier = tier,
    status = status.name.lowercase(),
    priority = priority,
    title = title,
    description = description,
    address = location.address,
    latitude = location.latitude,
    longitude = location.longitude,
    zoneId = location.zoneId,
    requirementsJson = json.encodeToString(requirements),
    crewId = assignment?.crewId,
    crewName = assignment?.crewName,
    assignedAt = assignment?.assignedAt,
    scheduledDate = assignment?.scheduledDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

@kotlinx.serialization.Serializable
data class StepJson(
    val id: String,
    val sequence: Int,
    val label: String,
    val description: String,
    val isCompleted: Boolean,
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    workOrderId = workOrderId,
    sequence = sequence,
    type = type,
    label = label,
    description = description,
    estimatedMinutes = estimatedMinutes,
    status = status.name.lowercase(),
    stepsJson = steps.toStepsJson(),
    checkpointRequired = checkpointRequired,
    voiceGuidance = voiceGuidance,
    spliceDetailJson = spliceDetail?.let {
        json.encodeToString(
            mapOf("fiberCount" to it.fiberCount.toString(), "spliceType" to it.spliceType, "enclosureId" to (it.enclosureId ?: ""))
        )
    },
    cablePullDetailJson = cablePullDetail?.let {
        json.encodeToString(
            mapOf("cableType" to it.cableType, "lengthMeters" to it.lengthMeters.toString(), "startPoint" to it.startPoint, "endPoint" to it.endPoint)
        )
    },
)

fun List<TaskStep>.toStepsJson(): String = json.encodeToString(
    map { StepJson(it.id, it.sequence, it.label, it.description, it.isCompleted) }
)

// --- Entity -> Domain ---

fun WorkOrderEntity.toDomain(): WorkOrder = WorkOrder(
    id = id,
    type = WorkOrderType.valueOf(type.uppercase()),
    baselineId = baselineId,
    projectId = projectId,
    tier = tier,
    status = WorkOrderStatus.valueOf(status.uppercase()),
    priority = priority,
    title = title,
    description = description,
    location = WorkOrderLocation(
        address = address,
        latitude = latitude,
        longitude = longitude,
        zoneId = zoneId,
    ),
    requirements = try { json.decodeFromString(requirementsJson) } catch (_: Exception) { emptyList() },
    assignment = crewId?.let {
        WorkOrderAssignment(
            crewId = it,
            crewName = crewName ?: "",
            assignedAt = assignedAt ?: "",
            scheduledDate = scheduledDate,
        )
    },
    tasks = emptyList(),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// --- TaskPhoto Entity <-> Domain ---

fun TaskPhotoEntity.toDomain(): TaskPhoto = TaskPhoto(
    id = id,
    taskId = taskId,
    stepId = stepId,
    workOrderId = workOrderId,
    filePath = filePath,
    thumbnailPath = thumbnailPath,
    cameraFacing = CameraFacing.valueOf(cameraFacing.uppercase()),
    capturedAt = capturedAt,
    latitude = latitude,
    longitude = longitude,
    analysisStatus = PhotoAnalysisStatus.valueOf(analysisStatus.uppercase()),
    analysisResult = analysisResult,
)

fun TaskPhoto.toEntity(): TaskPhotoEntity = TaskPhotoEntity(
    id = id,
    taskId = taskId,
    stepId = stepId,
    workOrderId = workOrderId,
    filePath = filePath,
    thumbnailPath = thumbnailPath,
    cameraFacing = cameraFacing.name.lowercase(),
    capturedAt = capturedAt,
    latitude = latitude,
    longitude = longitude,
    analysisStatus = analysisStatus.name.lowercase(),
    analysisResult = analysisResult,
)

// --- ChatMessage Entity <-> Domain ---

fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    taskId = taskId,
    role = ChatRole.valueOf(role.uppercase()),
    content = content,
    photoId = photoId,
    createdAt = createdAt,
)

fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    taskId = taskId,
    role = role.name.lowercase(),
    content = content,
    photoId = photoId,
    createdAt = createdAt,
)

fun TaskEntity.toDomain(): Task {
    val steps = try {
        json.decodeFromString<List<StepJson>>(stepsJson).map {
            TaskStep(it.id, it.sequence, it.label, it.description, it.isCompleted)
        }
    } catch (_: Exception) {
        emptyList()
    }

    val splice = spliceDetailJson?.let {
        try {
            val map = json.decodeFromString<Map<String, String>>(it)
            SpliceDetail(
                fiberCount = map["fiberCount"]?.toIntOrNull() ?: 0,
                spliceType = map["spliceType"] ?: "",
                enclosureId = map["enclosureId"]?.ifEmpty { null },
            )
        } catch (_: Exception) { null }
    }

    val cablePull = cablePullDetailJson?.let {
        try {
            val map = json.decodeFromString<Map<String, String>>(it)
            CablePullDetail(
                cableType = map["cableType"] ?: "",
                lengthMeters = map["lengthMeters"]?.toDoubleOrNull() ?: 0.0,
                startPoint = map["startPoint"] ?: "",
                endPoint = map["endPoint"] ?: "",
            )
        } catch (_: Exception) { null }
    }

    return Task(
        id = id,
        workOrderId = workOrderId,
        sequence = sequence,
        type = type,
        label = label,
        description = description,
        estimatedMinutes = estimatedMinutes,
        status = TaskStatus.valueOf(status.uppercase()),
        steps = steps,
        checkpointRequired = checkpointRequired,
        voiceGuidance = voiceGuidance,
        spliceDetail = splice,
        cablePullDetail = cablePull,
    )
}
