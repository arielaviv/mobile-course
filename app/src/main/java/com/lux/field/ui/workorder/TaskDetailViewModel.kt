package com.lux.field.ui.workorder

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lux.field.data.remote.ElevenLabsApiService
import com.lux.field.data.repository.ChatRepository
import com.lux.field.data.repository.PhotoRepository
import com.lux.field.data.repository.PreferencesRepository
import com.lux.field.service.LocationTrackingService
import com.lux.field.domain.model.CameraFacing
import com.lux.field.domain.model.ChatMessage
import com.lux.field.domain.model.ChatRole
import com.lux.field.domain.model.PhotoAnalysisStatus
import com.lux.field.domain.model.Task
import com.lux.field.domain.model.TaskPhoto
import com.lux.field.domain.model.TaskStatus
import com.lux.field.domain.usecase.UpdateTaskStatusUseCase
import com.lux.field.data.repository.TaskRepository
import com.lux.field.domain.voice.AudioPlayerManager
import com.lux.field.domain.voice.PlaybackState
import com.lux.field.domain.voice.SpeechRecognizerWrapper
import com.lux.field.domain.voice.SpeechState
import com.lux.field.ui.workorder.components.TaskChatContext
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val autoSpeak: Boolean = false,
    val taskChatContext: TaskChatContext? = null,
    val photoPathMap: Map<String, String> = emptyMap(),
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val updateTaskStatusUseCase: UpdateTaskStatusUseCase,
    private val photoRepository: PhotoRepository,
    private val chatRepository: ChatRepository,
    private val speechRecognizerWrapper: SpeechRecognizerWrapper,
    private val audioPlayerManager: AudioPlayerManager,
    private val elevenLabsApiService: ElevenLabsApiService,
    private val preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val workOrderId: String = savedStateHandle["workOrderId"] ?: ""
    val taskId: String = savedStateHandle["taskId"] ?: ""

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    val speechState: StateFlow<SpeechState> = speechRecognizerWrapper.state
    val playbackState: StateFlow<PlaybackState> = audioPlayerManager.state

    private var lastMessageCount = 0

    init {
        loadTask()
        observePhotos()
        observeChatMessages()
        observeAutoSpeak()
    }

    private fun loadTask() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = taskRepository.getTask(taskId)
            result.fold(
                onSuccess = { task ->
                    _uiState.update {
                        it.copy(
                            task = task,
                            isLoading = false,
                            taskChatContext = buildTaskChatContext(task),
                        )
                    }
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
                _uiState.update {
                    it.copy(
                        photos = photos,
                        photoPathMap = photos.associate { photo -> photo.id to photo.filePath },
                    )
                }
            }
        }
    }

    private fun observeChatMessages() {
        viewModelScope.launch {
            chatRepository.observeMessages(taskId).collect { messages ->
                val previousCount = lastMessageCount
                lastMessageCount = messages.size

                _uiState.update { it.copy(chatMessages = messages) }

                // Auto-speak new assistant messages
                if (_uiState.value.autoSpeak && messages.size > previousCount) {
                    val lastMessage = messages.lastOrNull()
                    if (lastMessage?.role == ChatRole.ASSISTANT) {
                        speakMessage(lastMessage)
                    }
                }
            }
        }
    }

    private fun observeAutoSpeak() {
        viewModelScope.launch {
            preferencesRepository.autoSpeak.collect { enabled ->
                _uiState.update { it.copy(autoSpeak = enabled) }
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
        LocationTrackingService.boostInterval(context)
        updateStatus(TaskStatus.IN_PROGRESS)
    }

    fun completeTask() {
        LocationTrackingService.normalInterval(context)
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
        viewModelScope.launch {
            // Insert photo as user chat message
            val photo = photoRepository.getPhoto(photoId) ?: return@launch
            chatRepository.insertPhotoMessage(
                taskId = taskId,
                photoId = photoId,
                cameraFacing = photo.cameraFacing.name.lowercase(),
            )
            // Keep chat open so it re-shows when returning from camera
            _uiState.update { it.copy(isChatOpen = true) }
            // Trigger analysis
            requestPhotoAnalysis(photoId)
        }
    }

    private fun requestPhotoAnalysis(photoId: String) {
        viewModelScope.launch {
            val photo = photoRepository.getPhoto(photoId) ?: return@launch
            photoRepository.updateAnalysis(photoId, PhotoAnalysisStatus.PENDING, null)
            _uiState.update { it.copy(isChatLoading = true) }

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
                    _uiState.update { it.copy(isChatLoading = false) }
                },
                onFailure = {
                    photoRepository.updateAnalysis(photoId, PhotoAnalysisStatus.FAILED, null)
                    _uiState.update { it.copy(isChatLoading = false) }
                },
            )
        }
    }

    // Voice methods
    fun startListening() {
        speechRecognizerWrapper.startListening()
    }

    fun stopListening() {
        speechRecognizerWrapper.stopListening()
    }

    fun speakMessage(message: ChatMessage) {
        viewModelScope.launch {
            val result = elevenLabsApiService.textToSpeech(message.content)
            result.fold(
                onSuccess = { audioBytes ->
                    audioPlayerManager.play(message.id, audioBytes)
                },
                onFailure = { /* silently fail TTS */ },
            )
        }
    }

    fun stopPlayback() {
        audioPlayerManager.stop()
    }

    fun toggleAutoSpeak() {
        val current = _uiState.value.autoSpeak
        preferencesRepository.setAutoSpeak(!current)
    }

    private fun buildTaskChatContext(task: Task): TaskChatContext {
        val completedSteps = task.steps.count { it.isCompleted }
        val currentStep = task.steps.firstOrNull { !it.isCompleted }?.label
        return TaskChatContext(
            taskLabel = task.label,
            taskType = task.type,
            currentStep = currentStep,
            progress = "$completedSteps/${task.steps.size} steps",
        )
    }

    val workPhotoCount: Int
        get() = _uiState.value.photos.count { it.cameraFacing == CameraFacing.BACK }

    val selfieCount: Int
        get() = _uiState.value.photos.count { it.cameraFacing == CameraFacing.FRONT }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerWrapper.destroy()
        audioPlayerManager.destroy()
    }
}
