package com.lux.field.ui.workorder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lux.field.domain.model.Task
import com.lux.field.domain.model.TaskStatus
import com.lux.field.domain.usecase.UpdateTaskStatusUseCase
import com.lux.field.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val statusUpdateSuccess: Boolean = false,
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val updateTaskStatusUseCase: UpdateTaskStatusUseCase,
) : ViewModel() {

    private val workOrderId: String = savedStateHandle["workOrderId"] ?: ""
    private val taskId: String = savedStateHandle["taskId"] ?: ""

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        loadTask()
    }

    private fun loadTask() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = taskRepository.getTask(taskId)
            result.fold(
                onSuccess = { task ->
                    _uiState.update { it.copy(task = task, isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    fun toggleStepCompletion(stepId: String, completed: Boolean) {
        viewModelScope.launch {
            updateTaskStatusUseCase.toggleStepCompletion(taskId, stepId, completed)
            loadTask()
        }
    }

    fun startTask() {
        updateStatus(TaskStatus.IN_PROGRESS)
    }

    fun completeTask() {
        updateStatus(TaskStatus.COMPLETED)
    }

    private fun updateStatus(newStatus: TaskStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = updateTaskStatusUseCase.updateStatus(
                workOrderId = workOrderId,
                taskId = taskId,
                newStatus = newStatus,
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(statusUpdateSuccess = true) }
                    loadTask()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }
}
