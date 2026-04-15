package com.field.survey.data.remote

import com.field.survey.data.remote.dto.ClaudeRequestDto
import com.field.survey.data.remote.dto.ClaudeResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AnthropicApiService {

    @POST("v1/messages")
    suspend fun createMessage(
        @Body request: ClaudeRequestDto,
    ): ClaudeResponseDto
}
