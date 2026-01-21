package com.lux.field.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
)
