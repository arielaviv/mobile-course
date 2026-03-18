package com.field.survey.ui.dp

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.field.survey.data.repository.AuthRepository
import com.field.survey.data.repository.DistributionPointRepository
import com.field.survey.data.repository.TokenProvider
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.DpType
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

enum class LocationMode {
    GPS,
    ADDRESS,
    MANUAL,
}

data class AddDpUiState(
    val label: String = "",
    val type: DpType = DpType.DP,
    val notes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val addressQuery: String = "",
    val locationMode: LocationMode = LocationMode.GPS,
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
    private val authRepository: AuthRepository,
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
                            locationMode = LocationMode.GPS,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingLocation = false,
                            locationError = "GPS unavailable. Use address or manual coordinates.",
                        )
                    }
                }
            } catch (e: SecurityException) {
                _uiState.update {
                    it.copy(
                        isLoadingLocation = false,
                        locationError = "Location permission denied. Use address or manual input.",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingLocation = false,
                        locationError = "GPS unavailable. Use address or manual coordinates.",
                    )
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

    fun setLocationMode(mode: LocationMode) {
        _uiState.update { it.copy(locationMode = mode, locationError = null) }
    }

    fun onAddressChanged(address: String) {
        _uiState.update { it.copy(addressQuery = address, error = null) }
    }

    fun searchAddress() {
        val address = _uiState.value.addressQuery.trim()
        if (address.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true, locationError = null) }
            try {
                val results = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    Geocoder(context, Locale.getDefault()).getFromLocationName(address, 1)
                }
                val location = results?.firstOrNull()
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
                        it.copy(isLoadingLocation = false, locationError = "Address not found")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoadingLocation = false, locationError = "Could not search address")
                }
            }
        }
    }

    fun setManualCoordinates(lat: Double, lng: Double) {
        _uiState.update {
            it.copy(
                latitude = lat,
                longitude = lng,
                locationError = null,
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.label.isBlank()) {
            _uiState.update { it.copy(error = "Label is required") }
            return
        }
        if (state.latitude == null || state.longitude == null) {
            _uiState.update { it.copy(error = "Location is required") }
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
                    imageBase64 = null,
                    notes = state.notes.trim(),
                    createdAt = System.currentTimeMillis(),
                    createdBy = authRepository.getUserId(),
                    createdByName = authRepository.getUserName(),
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
