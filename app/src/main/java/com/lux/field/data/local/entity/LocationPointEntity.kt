package com.lux.field.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_points",
    indices = [
        Index("timestamp"),
        Index("isSynced"),
    ],
)
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float,
    val bearing: Float,
    val altitude: Double,
    val timestamp: Long,
    val isSynced: Boolean = false,
)
