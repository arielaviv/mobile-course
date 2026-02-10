package com.lux.field.domain.model

data class TaskPhoto(
    val id: String,
    val taskId: String,
    val stepId: String?,
    val workOrderId: String,
    val filePath: String,
    val thumbnailPath: String?,
    val cameraFacing: CameraFacing,
    val capturedAt: Long,
    val latitude: Double?,
    val longitude: Double?,
    val analysisStatus: PhotoAnalysisStatus,
    val analysisResult: String?,
)

enum class CameraFacing {
    BACK,
    FRONT,
}

enum class PhotoAnalysisStatus {
    NONE,
    PENDING,
    COMPLETED,
    FAILED,
}
