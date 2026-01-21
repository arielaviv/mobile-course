package com.lux.field.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val workOrderId: String,
    val sequence: Int,
    val type: String,
    val label: String,
    val description: String,
    val estimatedMinutes: Int,
    val status: String,
    val stepsJson: String,
    val checkpointRequired: Boolean,
    val voiceGuidance: String?,
    val spliceDetailJson: String?,
    val cablePullDetailJson: String?,
    val lastSyncedAt: Long = System.currentTimeMillis(),
)
