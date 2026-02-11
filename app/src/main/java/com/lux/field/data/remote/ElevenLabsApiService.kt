package com.lux.field.data.remote

import com.lux.field.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ElevenLabsApiService(
    private val apiKey: String = BuildConfig.ELEVENLABS_API_KEY,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun textToSpeech(
        text: String,
        voiceId: String = DEFAULT_VOICE_ID,
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val requestBody = TtsRequest(
                text = text,
                modelId = "eleven_multilingual_v2",
                voiceSettings = VoiceSettings(stability = 0.5f, similarityBoost = 0.75f),
            )
            val body = json.encodeToString(requestBody)
                .toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url("$BASE_URL/text-to-speech/$voiceId")
                .addHeader("xi-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "audio/mpeg")
                .post(body)
                .build()

            val response = client.newCall(httpRequest).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                return@withContext Result.failure(
                    Exception("ElevenLabs API error ${response.code}: $errorBody")
                )
            }

            val audioBytes = response.body?.bytes()
                ?: return@withContext Result.failure(Exception("Empty audio response"))

            Result.success(audioBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val BASE_URL = "https://api.elevenlabs.io/v1"
        private const val DEFAULT_VOICE_ID = "TxGEqnHWrfWFTfGW9XjX" // Josh
    }
}

@Serializable
private data class TtsRequest(
    val text: String,
    @kotlinx.serialization.SerialName("model_id") val modelId: String,
    @kotlinx.serialization.SerialName("voice_settings") val voiceSettings: VoiceSettings,
)

@Serializable
private data class VoiceSettings(
    val stability: Float,
    @kotlinx.serialization.SerialName("similarity_boost") val similarityBoost: Float,
)
