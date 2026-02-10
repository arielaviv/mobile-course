package com.lux.field.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [
        Index("taskId"),
    ],
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val role: String,
    val content: String,
    val photoId: String?,
    val createdAt: Long,
)
