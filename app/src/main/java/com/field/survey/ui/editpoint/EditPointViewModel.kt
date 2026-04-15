package com.field.survey.ui.editpoint

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.field.survey.R
import com.field.survey.data.repository.DistributionPointRepository
import com.field.survey.data.repository.PhotoAnalysisRepository
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.DpType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditPointViewModel
    @Inject
    constructor(
        private val repository: DistributionPointRepository,
        private val photoAnalysisRepository: PhotoAnalysisRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {

        private val pointId: String = savedStateHandle["pointId"] ?: ""

        private val _point = MutableLiveData<DistributionPoint?>()
        val point: LiveData<DistributionPoint?> = _point

        private val _isSaving = MutableLiveData(false)
        val isSaving: LiveData<Boolean> = _isSaving

        private val _saveSuccess = MutableLiveData(false)
        val saveSuccess: LiveData<Boolean> = _saveSuccess

        private val _error = MutableLiveData<Int?>(null)
        val error: LiveData<Int?> = _error

        private val _isLoading = MutableLiveData(true)
        val isLoading: LiveData<Boolean> = _isLoading

        private val _photoPath = MutableLiveData<String?>(null)
        val photoPath: LiveData<String?> = _photoPath

        private val _isAnalyzing = MutableLiveData(false)
        val isAnalyzing: LiveData<Boolean> = _isAnalyzing

        private val _aiNotes = MutableLiveData<String?>(null)
        val aiNotes: LiveData<String?> = _aiNotes

        private val _aiError = MutableLiveData<Int?>(null)
        val aiError: LiveData<Int?> = _aiError

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
            _photoPath.value = path
        }

        fun analyzePhoto() {
            val path = _photoPath.value
            if (path.isNullOrBlank()) {
                _aiError.value = R.string.ai_analyze_no_photo
                return
            }
            if (_isAnalyzing.value == true) return

            _isAnalyzing.value = true
            _aiError.value = null
            viewModelScope.launch {
                val result = photoAnalysisRepository.analyzePhoto(path)
                _isAnalyzing.value = false
                result
                    .onSuccess { _aiNotes.value = it }
                    .onFailure { _aiError.value = R.string.ai_analyze_error }
            }
        }

        fun consumeAiNotes() {
            _aiNotes.value = null
        }

        fun consumeAiError() {
            _aiError.value = null
        }

        fun save(
            label: String,
            notes: String,
            type: DpType,
        ) {
            val current = _point.value ?: return

            if (label.isBlank()) {
                _error.value = R.string.point_label_required
                return
            }

            _isSaving.value = true
            _error.value = null

            viewModelScope.launch {
                try {
                    val updated =
                        current.copy(
                            label = label.trim(),
                            notes = notes.trim(),
                            type = type,
                            photoPath = _photoPath.value ?: current.photoPath,
                        )
                    repository.update(updated)
                    _saveSuccess.value = true
                } catch (_: Exception) {
                    _error.value = R.string.error_generic
                } finally {
                    _isSaving.value = false
                }
            }
        }
    }
