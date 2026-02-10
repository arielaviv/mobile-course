package com.lux.field.data.repository

import android.util.Base64
import com.lux.field.BuildConfig
import com.lux.field.data.local.dao.ChatMessageDao
import com.lux.field.data.mock.MockDataProvider
import com.lux.field.data.remote.ClaudeApiService
import com.lux.field.data.remote.dto.ClaudeContentBlock
import com.lux.field.data.remote.dto.ClaudeMessageDto
import com.lux.field.data.remote.dto.ClaudeMessagesRequest
import com.lux.field.data.remote.dto.ImageSource
import com.lux.field.domain.model.ChatMessage
import com.lux.field.domain.model.ChatRole
import com.lux.field.domain.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val claudeApiService: ClaudeApiService,
    private val mockDataProvider: MockDataProvider,
) {

    fun observeMessages(taskId: String): Flow<List<ChatMessage>> =
        chatMessageDao.observeByTask(taskId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun sendMessage(
        taskId: String,
        userText: String,
        task: Task?,
    ): Result<ChatMessage> {
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            role = ChatRole.USER,
            content = userText,
            photoId = null,
            createdAt = System.currentTimeMillis(),
        )
        chatMessageDao.insert(userMessage.toEntity())

        return if (BuildConfig.USE_MOCK_API) {
            val mockReply = mockDataProvider.getMockChatResponse(userText)
            val assistantMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                role = ChatRole.ASSISTANT,
                content = mockReply,
                photoId = null,
                createdAt = System.currentTimeMillis(),
            )
            chatMessageDao.insert(assistantMessage.toEntity())
            Result.success(assistantMessage)
        } else {
            sendToClaudeApi(taskId, task)
        }
    }

    suspend fun analyzePhoto(
        taskId: String,
        photoFilePath: String,
        photoId: String,
        cameraFacing: String,
        task: Task?,
    ): Result<ChatMessage> {
        if (BuildConfig.USE_MOCK_API) {
            val mockAnalysis = mockDataProvider.getMockPhotoAnalysis(cameraFacing)
            val message = ChatMessage(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                role = ChatRole.ASSISTANT,
                content = mockAnalysis,
                photoId = photoId,
                createdAt = System.currentTimeMillis(),
            )
            chatMessageDao.insert(message.toEntity())
            return Result.success(message)
        }

        return withContext(Dispatchers.IO) {
            try {
                val imageBytes = File(photoFilePath).readBytes()
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                val history = chatMessageDao.getByTask(taskId).map { it.toDomain() }
                val messages = buildApiMessages(history) + ClaudeMessageDto(
                    role = "user",
                    content = listOf(
                        ClaudeContentBlock.Image(
                            source = ImageSource(data = base64Image),
                        ),
                        ClaudeContentBlock.Text(
                            text = if (cameraFacing == "back") {
                                "Analyze this work photo. What do you see? Is the work quality acceptable?"
                            } else {
                                "This is a proof-of-presence selfie from the job site. Confirm you can see the technician."
                            },
                        ),
                    ),
                )

                val request = ClaudeMessagesRequest(
                    system = buildSystemPrompt(task),
                    messages = messages,
                )

                val result = claudeApiService.sendMessage(request)
                result.fold(
                    onSuccess = { response ->
                        val text = response.content
                            .firstOrNull { it.type == "text" }?.text
                            ?: "No analysis available."
                        val message = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            taskId = taskId,
                            role = ChatRole.ASSISTANT,
                            content = text,
                            photoId = photoId,
                            createdAt = System.currentTimeMillis(),
                        )
                        chatMessageDao.insert(message.toEntity())
                        Result.success(message)
                    },
                    onFailure = { Result.failure(it) },
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun sendToClaudeApi(
        taskId: String,
        task: Task?,
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val history = chatMessageDao.getByTask(taskId).map { it.toDomain() }
            val messages = buildApiMessages(history)

            val request = ClaudeMessagesRequest(
                system = buildSystemPrompt(task),
                messages = messages,
            )

            val result = claudeApiService.sendMessage(request)
            result.fold(
                onSuccess = { response ->
                    val text = response.content
                        .firstOrNull { it.type == "text" }?.text
                        ?: "I couldn't generate a response."
                    val message = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        taskId = taskId,
                        role = ChatRole.ASSISTANT,
                        content = text,
                        photoId = null,
                        createdAt = System.currentTimeMillis(),
                    )
                    chatMessageDao.insert(message.toEntity())
                    Result.success(message)
                },
                onFailure = { Result.failure(it) },
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSystemPrompt(task: Task?): String {
        val taskContext = task?.let {
            """
            Current task: ${it.label}
            Description: ${it.description}
            Type: ${it.type}
            Steps: ${it.steps.joinToString("; ") { s -> "${s.label} (${if (s.isCompleted) "done" else "pending"})" }}
            """.trimIndent()
        } ?: ""

        return """
            You are a helpful field technician assistant for Lux Field, a fiber optic installation app.
            You help technicians with their current task by answering questions about fiber optic installation,
            splicing, cable pulling, testing procedures, and safety.
            Keep answers concise and practical — the technician is in the field.
            $taskContext
        """.trimIndent()
    }

    private fun buildApiMessages(history: List<ChatMessage>): List<ClaudeMessageDto> =
        history
            .filter { it.role != ChatRole.SYSTEM }
            .map { msg ->
                ClaudeMessageDto(
                    role = when (msg.role) {
                        ChatRole.USER -> "user"
                        ChatRole.ASSISTANT -> "assistant"
                        ChatRole.SYSTEM -> "user"
                    },
                    content = listOf(ClaudeContentBlock.Text(text = msg.content)),
                )
            }
}
