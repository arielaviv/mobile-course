package com.lux.field.domain.usecase

import com.lux.field.data.repository.TaskRepository
import com.lux.field.domain.model.TaskStatus
import javax.inject.Inject

class UpdateTaskStatusUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend fun updateStatus(
        workOrderId: String,
        taskId: String,
        newStatus: TaskStatus,
        notes: String? = null,
    ): Result<Unit> {
        if (!isValidTransition(taskRepository.getTask(taskId).getOrNull()?.status, newStatus)) {
            return Result.failure(IllegalStateException("Invalid status transition to $newStatus"))
        }
        return taskRepository.updateTaskStatus(workOrderId, taskId, newStatus, notes)
    }

    suspend fun toggleStepCompletion(taskId: String, stepId: String, completed: Boolean): Result<Unit> {
        return taskRepository.updateStepCompletion(taskId, stepId, completed)
    }

    private fun isValidTransition(current: TaskStatus?, target: TaskStatus): Boolean {
        if (current == null) return false
        return when (current) {
            TaskStatus.PENDING -> target in listOf(TaskStatus.IN_PROGRESS, TaskStatus.SKIPPED, TaskStatus.CANCELLED)
            TaskStatus.IN_PROGRESS -> target in listOf(TaskStatus.COMPLETED, TaskStatus.BLOCKED, TaskStatus.ESCALATED, TaskStatus.CANCELLED)
            TaskStatus.BLOCKED -> target in listOf(TaskStatus.IN_PROGRESS, TaskStatus.ESCALATED, TaskStatus.CANCELLED)
            TaskStatus.ESCALATED -> target in listOf(TaskStatus.IN_PROGRESS, TaskStatus.COA_PENDING, TaskStatus.CANCELLED)
            TaskStatus.COA_PENDING -> target in listOf(TaskStatus.COA_APPROVED, TaskStatus.COA_REJECTED)
            TaskStatus.COA_APPROVED -> target in listOf(TaskStatus.IN_PROGRESS)
            TaskStatus.COA_REJECTED -> target in listOf(TaskStatus.BLOCKED, TaskStatus.CANCELLED)
            TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.SKIPPED -> false
        }
    }
}
