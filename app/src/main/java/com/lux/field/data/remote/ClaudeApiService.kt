package com.lux.field.data.remote

import com.lux.field.BuildConfig
import com.lux.field.data.remote.dto.ClaudeMessagesRequest
import com.lux.field.data.remote.dto.ClaudeMessagesResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ClaudeApiService(
    private val apiKey: String = BuildConfig.CLAUDE_API_KEY,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(request: ClaudeMessagesRequest): Result<ClaudeMessagesResponse> =
        withContext(Dispatchers.IO) {
            try {
                val body = json.encodeToString(request)
                    .toRequestBody("application/json".toMediaType())

                val httpRequest = Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .post(body)
                    .build()

                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response body"))

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Claude API error ${response.code}: $responseBody")
                    )
                }

                val parsed = json.decodeFromString<ClaudeMessagesResponse>(responseBody)
                Result.success(parsed)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
