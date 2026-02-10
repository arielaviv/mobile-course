package com.lux.field.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_photos",
    indices = [
        Index("taskId"),
        Index("workOrderId"),
    ],
)
data class TaskPhotoEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val stepId: String?,
    val workOrderId: String,
    val filePath: String,
    val thumbnailPath: String?,
    val cameraFacing: String,
    val capturedAt: Long,
    val latitude: Double?,
    val longitude: Double?,
    val analysisStatus: String,
    val analysisResult: String?,
)
