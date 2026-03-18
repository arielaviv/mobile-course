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
import com.field.survey.domain.model.WorkOrder
import com.field.survey.domain.usecase.GetAssignedWorkOrdersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val workOrders: List<WorkOrder> = emptyList(),
    val distributionPoints: List<DistributionPoint> = emptyList(),
    val selectedWorkOrder: WorkOrder? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val userName: String = "",
    val loggedOut: Boolean = false,
    val weather: Weather? = null,
    val weatherError: String? = null,
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val getAssignedWorkOrdersUseCase: GetAssignedWorkOrdersUseCase,
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
        observeWorkOrders()
        observeDistributionPoints()
        syncDistributionPoints()
        refresh()
        fetchWeather()
    }

    private fun observeWorkOrders() {
        viewModelScope.launch {
            getAssignedWorkOrdersUseCase.observe().collect { workOrders ->
                _uiState.update { it.copy(workOrders = workOrders) }
            }
        }
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
            distributionPointRepository.syncFromFirestore()
        }
    }

    private fun fetchWeather() {
        viewModelScope.launch {
            val result = weatherRepository.getWeather(lat = DEFAULT_LAT, lon = DEFAULT_LON)
            result.fold(
                onSuccess = { weather ->
                    _uiState.update { it.copy(weather = weather, weatherError = null) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(weatherError = e.message) }
                },
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = getAssignedWorkOrdersUseCase.refresh()
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
        syncDistributionPoints()
        fetchWeather()
    }

    fun selectWorkOrder(workOrder: WorkOrder?) {
        _uiState.update { it.copy(selectedWorkOrder = workOrder) }
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
