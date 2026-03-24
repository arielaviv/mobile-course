package com.field.survey.ui.addpoint

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.field.survey.data.repository.AuthRepository
import com.field.survey.data.repository.DistributionPointRepository
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.DpType
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddPointViewModel @Inject constructor(
    private val repository: DistributionPointRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _selectedType = MutableLiveData(DpType.MANHOLE)
    val selectedType: LiveData<DpType> = _selectedType

    private val _photoPath = MutableLiveData<String?>(null)
    val photoPath: LiveData<String?> = _photoPath

    private val _latitude = MutableLiveData(0.0)
    val latitude: LiveData<Double> = _latitude

    private val _longitude = MutableLiveData(0.0)
    val longitude: LiveData<Double> = _longitude

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
        _photoPath.value = path
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
                }
            }
    }

    fun save(label: String, notes: String) {
        if (label.isBlank()) {
            _error.value = "Label is required"
            return
        }

        _isSaving.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val dp = DistributionPoint(
                    id = UUID.randomUUID().toString(),
                    label = label.trim(),
                    type = _selectedType.value ?: DpType.MANHOLE,
                    latitude = _latitude.value ?: 0.0,
                    longitude = _longitude.value ?: 0.0,
                    photoPath = _photoPath.value,
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
