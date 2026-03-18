package com.field.survey.ui.dp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.field.survey.data.repository.DistributionPointRepository
import com.field.survey.domain.model.DpType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditDpUiState(
    val label: String = "",
    val type: DpType = DpType.DP,
    val notes: String = "",
    val photoPath: String? = null,
    val imageBase64: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class EditDpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DistributionPointRepository,
) : ViewModel() {

    private val dpId: String = savedStateHandle["dpId"] ?: ""

    private val _uiState = MutableStateFlow(EditDpUiState())
    val uiState: StateFlow<EditDpUiState> = _uiState.asStateFlow()

    private var originalDp: com.field.survey.domain.model.DistributionPoint? = null

    init {
        loadDp()
    }

    private fun loadDp() {
        viewModelScope.launch {
            val dp = repository.getById(dpId)
            if (dp != null) {
                originalDp = dp
                _uiState.update {
                    it.copy(
                        label = dp.label,
                        type = dp.type,
                        notes = dp.notes,
                        photoPath = dp.photoPath,
                        imageBase64 = dp.imageBase64,
                        isLoading = false,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = "Point not found")
                }
            }
        }
    }

    fun onLabelChanged(label: String) {
        _uiState.update { it.copy(label = label, error = null) }
    }

    fun onTypeChanged(type: DpType) {
        _uiState.update { it.copy(type = type) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun onPhotoTaken(path: String) {
        _uiState.update { it.copy(photoPath = path, imageBase64 = null) }
    }

    fun save() {
        val state = _uiState.value
        val dp = originalDp ?: return

        if (state.label.isBlank()) {
            _uiState.update { it.copy(error = "Label is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val updated = dp.copy(
                    label = state.label.trim(),
                    type = state.type,
                    notes = state.notes.trim(),
                    photoPath = state.photoPath,
                    imageBase64 = state.imageBase64,
                )
                repository.update(updated)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: "Save failed")
                }
            }
        }
    }
}
