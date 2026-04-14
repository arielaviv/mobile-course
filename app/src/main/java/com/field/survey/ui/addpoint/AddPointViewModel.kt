package com.field.survey.ui.addpoint

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.field.survey.data.repository.AuthRepository
import com.field.survey.data.repository.DistributionPointRepository
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
    ) : ViewModel() {

        private val argType: String = savedStateHandle["dpType"] ?: ""
        private val initialType: DpType = runCatching { DpType.valueOf(argType) }.getOrDefault(DpType.POLES)

        val pathCoordinatesArg: String = savedStateHandle["pathCoordinates"] ?: ""
        private val vertices = PathCoordinates.decode(pathCoordinatesArg)

        val typeLocked: Boolean = argType.isNotEmpty()

        private val _selectedType = MutableLiveData(initialType)
        val selectedType: LiveData<DpType> = _selectedType

        private val photoPath = MutableLiveData<String?>(null)

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

        private val _error = MutableLiveData<String?>(null)
        val error: LiveData<String?> = _error

        fun setSelectedType(type: DpType) {
            _selectedType.value = type
        }

        fun setPhotoPath(path: String?) {
            photoPath.value = path
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
                _error.value = "Label is required"
                return
            }

            val type = _selectedType.value ?: initialType
            if (type.isLine && vertices.size < 2) {
                _error.value = "Line needs at least 2 points"
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
                            photoPath = photoPath.value,
                            imageBase64 = null,
                            notes = notes.trim(),
                            createdAt = System.currentTimeMillis(),
                            createdBy = authRepository.getUserId(),
                            createdByName = authRepository.getUserName(),
                        )
                    repository.save(dp)
                    _saveSuccess.value = true
                } catch (e: Exception) {
                    _error.value = e.message ?: "Failed to save point"
                } finally {
                    _isSaving.value = false
                }
            }
        }
    }
