package com.lux.field.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClaudeMessagesRequest(
    val model: String = "claude-sonnet-4-5-20250929",
    @SerialName("max_tokens") val maxTokens: Int = 1024,
    val system: String? = null,
    val messages: List<ClaudeMessageDto>,
)

@Serializable
data class ClaudeMessageDto(
    val role: String,
    val content: List<ClaudeContentBlock>,
)

@Serializable
sealed interface ClaudeContentBlock {

    @Serializable
    @SerialName("text")
    data class Text(
        val type: String = "text",
        val text: String,
    ) : ClaudeContentBlock

    @Serializable
    @SerialName("image")
    data class Image(
        val type: String = "image",
        val source: ImageSource,
    ) : ClaudeContentBlock
}

@Serializable
data class ImageSource(
    val type: String = "base64",
    @SerialName("media_type") val mediaType: String = "image/jpeg",
    val data: String,
)

@Serializable
data class ClaudeMessagesResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeResponseContent>,
    val model: String,
    @SerialName("stop_reason") val stopReason: String?,
)

@Serializable
data class ClaudeResponseContent(
    val type: String,
    val text: String? = null,
)
