package com.lux.field.ui.workorder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lux.field.data.repository.ChatRepository
import com.lux.field.data.repository.PhotoRepository
import com.lux.field.domain.model.CameraFacing
import com.lux.field.domain.model.ChatMessage
import com.lux.field.domain.model.PhotoAnalysisStatus
import com.lux.field.domain.model.Task
import com.lux.field.domain.model.TaskPhoto
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
    val photos: List<TaskPhoto> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val isChatOpen: Boolean = false,
    val isChatLoading: Boolean = false,
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val updateTaskStatusUseCase: UpdateTaskStatusUseCase,
    private val photoRepository: PhotoRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    val workOrderId: String = savedStateHandle["workOrderId"] ?: ""
    val taskId: String = savedStateHandle["taskId"] ?: ""

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        loadTask()
        observePhotos()
        observeChatMessages()
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

    private fun observePhotos() {
        viewModelScope.launch {
            photoRepository.observePhotos(taskId).collect { photos ->
                _uiState.update { it.copy(photos = photos) }
            }
        }
    }

    private fun observeChatMessages() {
        viewModelScope.launch {
            chatRepository.observeMessages(taskId).collect { messages ->
                _uiState.update { it.copy(chatMessages = messages) }
            }
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

    fun openChat() {
        _uiState.update { it.copy(isChatOpen = true) }
    }

    fun closeChat() {
        _uiState.update { it.copy(isChatOpen = false) }
    }

    fun sendChatMessage(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChatLoading = true) }
            val result = chatRepository.sendMessage(
                taskId = taskId,
                userText = text,
                task = _uiState.value.task,
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isChatLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isChatLoading = false, error = e.message) }
                },
            )
        }
    }

    fun onPhotoSaved(photoId: String) {
        requestPhotoAnalysis(photoId)
    }

    private fun requestPhotoAnalysis(photoId: String) {
        viewModelScope.launch {
            val photo = photoRepository.getPhoto(photoId) ?: return@launch
            photoRepository.updateAnalysis(photoId, PhotoAnalysisStatus.PENDING, null)

            val result = chatRepository.analyzePhoto(
                taskId = taskId,
                photoFilePath = photo.filePath,
                photoId = photoId,
                cameraFacing = photo.cameraFacing.name.lowercase(),
                task = _uiState.value.task,
            )

            result.fold(
                onSuccess = { message ->
                    photoRepository.updateAnalysis(
                        photoId,
                        PhotoAnalysisStatus.COMPLETED,
                        message.content,
                    )
                },
                onFailure = {
                    photoRepository.updateAnalysis(photoId, PhotoAnalysisStatus.FAILED, null)
                },
            )
        }
    }

    val workPhotoCount: Int
        get() = _uiState.value.photos.count { it.cameraFacing == CameraFacing.BACK }

    val selfieCount: Int
        get() = _uiState.value.photos.count { it.cameraFacing == CameraFacing.FRONT }
}
