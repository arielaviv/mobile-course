package com.field.survey.data.repository

import com.field.survey.BuildConfig
import com.field.survey.data.local.dao.TaskDao
import com.field.survey.data.mock.MockDataProvider
import com.field.survey.data.remote.FieldSurveyApi
import com.field.survey.data.remote.dto.TaskUpdateRequest
import com.field.survey.domain.model.Task
import com.field.survey.domain.model.TaskStatus
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val api: FieldSurveyApi,
    private val taskDao: TaskDao,
    private val mockDataProvider: MockDataProvider,
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
        return try {
            if (BuildConfig.USE_MOCK_API) {
                mockDataProvider.updateTaskStatus(taskId, status)
            } else {
                api.updateTaskStatus(
                    TaskUpdateRequest(
                        workOrderId = workOrderId,
                        taskId = taskId,
                        status = status,
                        notes = notes,
                        timestamp = Instant.now().toString(),
                    )
                )
            }
            taskDao.updateStatus(taskId, status.name.lowercase())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
