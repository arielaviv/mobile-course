package com.lux.field.ui.dp

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.lux.field.data.repository.DistributionPointRepository
import com.lux.field.data.repository.TokenProvider
import com.lux.field.domain.model.DistributionPoint
import com.lux.field.domain.model.DpType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

data class AddDpUiState(
    val label: String = "",
    val type: DpType = DpType.DP,
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isLoadingLocation: Boolean = true,
    val locationError: String? = null,
    val photoPath: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AddDpViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DistributionPointRepository,
    private val tokenProvider: TokenProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDpUiState())
    val uiState: StateFlow<AddDpUiState> = _uiState.asStateFlow()

    init {
        fetchLocation()
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true, locationError = null) }
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                val location = client.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token,
                ).await()
                if (location != null) {
                    _uiState.update {
                        it.copy(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            isLoadingLocation = false,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoadingLocation = false, locationError = "Could not get location")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoadingLocation = false, locationError = e.message ?: "Location error")
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
        _uiState.update { it.copy(photoPath = path) }
    }

    fun retryLocation() {
        fetchLocation()
    }

    fun save() {
        val state = _uiState.value
        if (state.label.isBlank()) {
            _uiState.update { it.copy(error = "Label is required") }
            return
        }
        if (state.latitude == null || state.longitude == null) {
            _uiState.update { it.copy(error = "Location not available") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val dp = DistributionPoint(
                    id = UUID.randomUUID().toString(),
                    label = state.label.trim(),
                    type = state.type,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    photoPath = state.photoPath,
                    notes = state.notes.trim(),
                    createdAt = System.currentTimeMillis(),
                    createdBy = tokenProvider.getUserId(),
                )
                repository.save(dp)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: "Save failed")
                }
            }
        }
    }
}
