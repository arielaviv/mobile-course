package com.field.survey.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.field.survey.data.repository.AuthRepository
import com.field.survey.data.repository.DistributionPointRepository
import com.field.survey.data.repository.MapStyle
import com.field.survey.data.repository.PreferencesRepository
import com.field.survey.data.repository.WeatherRepository
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.Weather
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val distributionPoints: List<DistributionPoint> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val userName: String = "",
    val loggedOut: Boolean = false,
    val weather: Weather? = null,
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val distributionPointRepository: DistributionPointRepository,
    private val weatherRepository: WeatherRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    val mapStyle: StateFlow<MapStyle> = preferencesRepository.mapStyle

    init {
        _uiState.update { it.copy(userName = authRepository.getUserName()) }
        observeDistributionPoints()
        syncDistributionPoints()
        fetchWeather()
    }

    private fun observeDistributionPoints() {
        viewModelScope.launch {
            distributionPointRepository.observeAll().collect { dps ->
                _uiState.update { it.copy(distributionPoints = dps) }
            }
        }
    }

    private fun syncDistributionPoints() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            distributionPointRepository.syncFromFirestore()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun fetchWeather() {
        viewModelScope.launch {
            val result = weatherRepository.getWeather(lat = DEFAULT_LAT, lon = DEFAULT_LON)
            result.onSuccess { weather ->
                _uiState.update { it.copy(weather = weather) }
            }
        }
    }

    fun refresh() {
        syncDistributionPoints()
        fetchWeather()
    }

    fun logout() {
        authRepository.logout()
        _uiState.update { it.copy(loggedOut = true) }
    }

    companion object {
        private const val DEFAULT_LAT = 32.0750
        private const val DEFAULT_LON = 34.7725
    }
}
