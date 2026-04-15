package com.field.survey.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClaudeRequestDto(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<ClaudeMessageDto>,
)

@Serializable
data class ClaudeMessageDto(
    val role: String,
    val content: List<ClaudeContentBlockDto>,
)

@Serializable
sealed class ClaudeContentBlockDto {

    @Serializable
    @SerialName("text")
    data class Text(val text: String) : ClaudeContentBlockDto()

    @Serializable
    @SerialName("image")
    data class Image(val source: ClaudeImageSourceDto) : ClaudeContentBlockDto()
}

@Serializable
data class ClaudeImageSourceDto(
    val type: String = "base64",
    @SerialName("media_type") val mediaType: String = "image/jpeg",
    val data: String,
)

@Serializable
data class ClaudeResponseDto(
    val content: List<ClaudeResponseBlockDto> = emptyList(),
)

@Serializable
data class ClaudeResponseBlockDto(
    val type: String = "text",
    val text: String = "",
)
