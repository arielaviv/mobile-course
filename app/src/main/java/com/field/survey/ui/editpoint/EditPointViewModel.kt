package com.field.survey.ui.editpoint

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.field.survey.data.repository.DistributionPointRepository
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.DpType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditPointViewModel @Inject constructor(
    private val repository: DistributionPointRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val pointId: String = savedStateHandle["pointId"] ?: ""

    private val _point = MutableLiveData<DistributionPoint?>()
    val point: LiveData<DistributionPoint?> = _point

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _saveSuccess = MutableLiveData(false)
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private var photoPath: String? = null

    init {
        loadPoint()
    }

    private fun loadPoint() {
        viewModelScope.launch {
            _isLoading.value = true
            _point.value = repository.getById(pointId)
            _isLoading.value = false
        }
    }

    fun setPhotoPath(path: String?) {
        photoPath = path
    }

    fun save(label: String, notes: String, type: DpType) {
        val current = _point.value ?: return

        if (label.isBlank()) {
            _error.value = "Label is required"
            return
        }

        _isSaving.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val updated = current.copy(
                    label = label.trim(),
                    notes = notes.trim(),
                    type = type,
                    photoPath = photoPath ?: current.photoPath,
                )
                repository.update(updated)
                _saveSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save changes"
            } finally {
                _isSaving.value = false
            }
        }
    }
}
