package com.lux.field.data.repository

import com.lux.field.BuildConfig
import com.lux.field.data.connectivity.ConnectivityObserver
import com.lux.field.data.local.dao.TaskDao
import com.lux.field.data.mock.MockDataProvider
import com.lux.field.data.remote.LuxApi
import com.lux.field.data.remote.dto.TaskUpdateRequest
import com.lux.field.data.sync.OfflineSyncManager
import com.lux.field.data.sync.TaskUpdatePayload
import com.lux.field.domain.model.Task
import com.lux.field.domain.model.TaskStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val api: LuxApi,
    private val taskDao: TaskDao,
    private val mockDataProvider: MockDataProvider,
    private val connectivityObserver: ConnectivityObserver,
    private val offlineSyncManager: OfflineSyncManager,
    private val json: Json,
) {
    suspend fun getTask(taskId: String): Result<Task> {
        return try {
            val entity = taskDao.getById(taskId)
                ?: return Result.failure(NoSuchElementException("Task not found: $taskId"))
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTaskStatus(
        workOrderId: String,
        taskId: String,
        status: TaskStatus,
        notes: String? = null,
    ): Result<Unit> {
        // Always update local DB immediately
        taskDao.updateStatus(taskId, status.name.lowercase())

        if (BuildConfig.USE_MOCK_API) {
            mockDataProvider.updateTaskStatus(taskId, status)
            return Result.success(Unit)
        }

        val timestamp = Instant.now().toString()

        return if (connectivityObserver.isOnline.value) {
            try {
                api.updateTaskStatus(
                    TaskUpdateRequest(
                        workOrderId = workOrderId,
                        taskId = taskId,
                        status = status,
                        notes = notes,
                        timestamp = timestamp,
                    )
                )
                Result.success(Unit)
            } catch (e: Exception) {
                // API failed — queue for retry
                queueTaskUpdate(workOrderId, taskId, status, notes, timestamp)
                Result.success(Unit)
            }
        } else {
            // Offline — queue for later
            queueTaskUpdate(workOrderId, taskId, status, notes, timestamp)
            Result.success(Unit)
        }
    }

    private suspend fun queueTaskUpdate(
        workOrderId: String,
        taskId: String,
        status: TaskStatus,
        notes: String?,
        timestamp: String,
    ) {
        val payload = TaskUpdatePayload(
            workOrderId = workOrderId,
            taskId = taskId,
            status = status.name.lowercase(),
            notes = notes,
            timestamp = timestamp,
        )
        offlineSyncManager.enqueue(
            type = OfflineSyncManager.TYPE_TASK_UPDATE,
            payloadJson = json.encodeToString(payload),
        )
    }

    suspend fun updateStepCompletion(taskId: String, stepId: String, completed: Boolean): Result<Unit> {
        return try {
            val entity = taskDao.getById(taskId)
                ?: return Result.failure(NoSuchElementException("Task not found: $taskId"))
            val task = entity.toDomain()
            val updatedSteps = task.steps.map { step ->
                if (step.id == stepId) step.copy(isCompleted = completed) else step
            }
            val updatedTask = task.copy(steps = updatedSteps)
            taskDao.updateSteps(taskId, updatedTask.steps.toStepsJson())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
