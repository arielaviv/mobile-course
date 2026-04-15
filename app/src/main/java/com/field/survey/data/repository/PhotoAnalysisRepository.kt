package com.field.survey.data.repository

import com.field.survey.data.remote.AnthropicApiService
import com.field.survey.data.remote.dto.ClaudeContentBlockDto
import com.field.survey.data.remote.dto.ClaudeImageSourceDto
import com.field.survey.data.remote.dto.ClaudeMessageDto
import com.field.survey.data.remote.dto.ClaudeRequestDto
import com.field.survey.ui.util.ImageCompression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoAnalysisRepository
    @Inject
    constructor(
        private val api: AnthropicApiService,
    ) {

        suspend fun analyzePhoto(photoPath: String): Result<String> {
            val base64 =
                withContext(Dispatchers.IO) {
                    ImageCompression.fileToCompressedBase64(photoPath)
                } ?: return Result.failure(IllegalStateException("Could not read photo"))

            return try {
                val response =
                    api.createMessage(
                        ClaudeRequestDto(
                            model = MODEL,
                            maxTokens = MAX_TOKENS,
                            messages =
                                listOf(
                                    ClaudeMessageDto(
                                        role = "user",
                                        content =
                                            listOf(
                                                ClaudeContentBlockDto.Image(
                                                    source = ClaudeImageSourceDto(data = base64),
                                                ),
                                                ClaudeContentBlockDto.Text(text = PROMPT),
                                            ),
                                    ),
                                ),
                        ),
                    )
                val text =
                    response.content
                        .firstOrNull { it.type == "text" }
                        ?.text
                        ?.trim()
                        .orEmpty()
                if (text.isBlank()) {
                    Result.failure(IllegalStateException("Empty response"))
                } else {
                    Result.success(text)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        companion object {
            private const val MODEL = "claude-haiku-4-5-20251001"
            private const val MAX_TOKENS = 120
            private const val PROMPT =
                "You are labeling a photo for a telecom field survey app. " +
                    "In ONE concise sentence (max 20 words), describe what is visible: " +
                    "object type (pole / cabinet / duct / aerial cable), condition, and any " +
                    "notable identifying features. No preamble, no markdown, plain sentence."
        }
    }
