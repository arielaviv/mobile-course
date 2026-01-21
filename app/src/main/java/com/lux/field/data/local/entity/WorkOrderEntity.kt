package com.lux.field.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_orders")
data class WorkOrderEntity(
    @PrimaryKey val id: String,
    val type: String,
    val baselineId: String?,
    val projectId: String,
    val tier: Int,
    val status: String,
    val priority: Int,
    val title: String,
    val description: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String?,
    val requirementsJson: String,
    val crewId: String?,
    val crewName: String?,
    val assignedAt: String?,
    val scheduledDate: String?,
    val createdAt: String,
    val updatedAt: String,
    val lastSyncedAt: Long = System.currentTimeMillis(),
)
