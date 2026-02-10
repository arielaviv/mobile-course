package com.lux.field.domain.model

data class ChatMessage(
    val id: String,
    val taskId: String,
    val role: ChatRole,
    val content: String,
    val photoId: String?,
    val createdAt: Long,
)

enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM,
}
