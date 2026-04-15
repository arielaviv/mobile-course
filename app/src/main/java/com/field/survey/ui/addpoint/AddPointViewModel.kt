package com.field.survey.ui.addpoint

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.field.survey.R
import com.field.survey.data.repository.AuthRepository
import com.field.survey.data.repository.DistributionPointRepository
import com.field.survey.data.repository.PhotoAnalysisRepository
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.DpType
import com.field.survey.domain.model.PathCoordinates
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddPointViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: DistributionPointRepository,
        private val authRepository: AuthRepository,
        private val photoAnalysisRepository: PhotoAnalysisRepository,
    ) : ViewModel() {

        private val argType: String = savedStateHandle["dpType"] ?: ""
        private val initialType: DpType = runCatching { DpType.valueOf(argType) }.getOrDefault(DpType.POLES)

        val pathCoordinatesArg: String = savedStateHandle["pathCoordinates"] ?: ""
        private val vertices = PathCoordinates.decode(pathCoordinatesArg)

        val typeLocked: Boolean = argType.isNotEmpty()

        private val _selectedType = MutableLiveData(initialType)
        val selectedType: LiveData<DpType> = _selectedType

        private val _photoPath = MutableLiveData<String?>(null)
        val photoPath: LiveData<String?> = _photoPath

        private val _latitude = MutableLiveData(0.0)
        val latitude: LiveData<Double> = _latitude

        private val _longitude = MutableLiveData(0.0)
        val longitude: LiveData<Double> = _longitude

        private val _accuracy = MutableLiveData<Float?>(null)
        val accuracy: LiveData<Float?> = _accuracy

        val vertexCount: Int get() = vertices.size

        init {
            val lat = savedStateHandle.get<Float>("latitude") ?: 0f
            val lng = savedStateHandle.get<Float>("longitude") ?: 0f
            if (lat != 0f || lng != 0f) {
                _latitude.value = lat.toDouble()
                _longitude.value = lng.toDouble()
            }
        }

        private val _isSaving = MutableLiveData(false)
        val isSaving: LiveData<Boolean> = _isSaving

        private val _saveSuccess = MutableLiveData(false)
        val saveSuccess: LiveData<Boolean> = _saveSuccess

        private val _error = MutableLiveData<Int?>(null)
        val error: LiveData<Int?> = _error

        private val _isAnalyzing = MutableLiveData(false)
        val isAnalyzing: LiveData<Boolean> = _isAnalyzing

        private val _aiNotes = MutableLiveData<String?>(null)
        val aiNotes: LiveData<String?> = _aiNotes

        private val _aiError = MutableLiveData<Int?>(null)
        val aiError: LiveData<Int?> = _aiError

        fun setSelectedType(type: DpType) {
            _selectedType.value = type
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

        @SuppressLint("MissingPermission")
        fun fetchLocation(context: Context) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cancellationToken = CancellationTokenSource()
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        _latitude.value = location.latitude
                        _longitude.value = location.longitude
                        _accuracy.value = if (location.hasAccuracy()) location.accuracy else null
                    }
                }
        }

        fun save(
            label: String,
            notes: String,
        ) {
            if (label.isBlank()) {
                _error.value = R.string.point_label_required
                return
            }

            val type = _selectedType.value ?: initialType
            if (type.isLine && vertices.size < 2) {
                _error.value = R.string.point_line_needs_two
                return
            }

            _isSaving.value = true
            _error.value = null

            viewModelScope.launch {
                try {
                    val dp =
                        DistributionPoint(
                            id = UUID.randomUUID().toString(),
                            label = label.trim(),
                            type = type,
                            latitude = _latitude.value ?: 0.0,
                            longitude = _longitude.value ?: 0.0,
                            pathCoordinates = if (type.isLine) pathCoordinatesArg.ifBlank { null } else null,
                            photoPath = _photoPath.value,
                            imageBase64 = null,
                            notes = notes.trim(),
                            createdAt = System.currentTimeMillis(),
                            createdBy = authRepository.getUserId(),
                            createdByName = authRepository.getUserName(),
                        )
                    repository.save(dp)
                    _saveSuccess.value = true
                } catch (_: Exception) {
                    _error.value = R.string.error_generic
                } finally {
                    _isSaving.value = false
                }
            }
        }
    }
