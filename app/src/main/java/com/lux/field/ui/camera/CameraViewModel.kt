package com.lux.field.ui.camera

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lux.field.data.repository.PhotoRepository
import com.lux.field.domain.model.CameraFacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CameraUiState(
    val cameraFacing: CameraFacing = CameraFacing.BACK,
    val isFlashOn: Boolean = false,
    val capturedPhotoFile: File? = null,
    val savedPhotoId: String? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
) : ViewModel() {

    val workOrderId: String = savedStateHandle["workOrderId"] ?: ""
    val taskId: String = savedStateHandle["taskId"] ?: ""
    private val stepId: String? = savedStateHandle["stepId"]
    val initialCameraFacing: CameraFacing = savedStateHandle.get<String>("cameraFacing")
        ?.let { CameraFacing.valueOf(it.uppercase()) }
        ?: CameraFacing.BACK

    private val _uiState = MutableStateFlow(CameraUiState(cameraFacing = initialCameraFacing))
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun toggleFlash() {
        _uiState.update { it.copy(isFlashOn = !it.isFlashOn) }
    }

    fun onPhotoCaptured(file: File) {
        _uiState.update { it.copy(capturedPhotoFile = file) }
    }

    fun onCaptureError(exception: ImageCaptureException) {
        _uiState.update { it.copy(error = exception.message) }
    }

    fun retake() {
        _uiState.value.capturedPhotoFile?.delete()
        _uiState.update { it.copy(capturedPhotoFile = null, error = null) }
    }

    fun usePhoto() {
        val file = _uiState.value.capturedPhotoFile ?: return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val photo = photoRepository.savePhoto(
                    sourceFile = file,
                    taskId = taskId,
                    stepId = stepId,
                    workOrderId = workOrderId,
                    cameraFacing = _uiState.value.cameraFacing,
                    latitude = null,
                    longitude = null,
                )
                file.delete()
                _uiState.update { it.copy(savedPhotoId = photo.id, isSaving = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isSaving = false) }
            }
        }
    }
}
