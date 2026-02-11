package com.lux.field.ui.navigation

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lux.field.data.repository.LocationRepository
import com.lux.field.data.repository.NavigationRepository
import com.lux.field.data.repository.NavigationRouteResult
import com.lux.field.service.LocationTrackingService
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.maneuver.model.Maneuver
import com.mapbox.navigation.tripdata.progress.api.MapboxTripProgressApi
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateValue
import com.mapbox.navigation.voice.api.MapboxSpeechApi
import com.mapbox.navigation.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NavigationUiState(
    val isLoading: Boolean = false,
    val isNavigating: Boolean = false,
    val currentManeuver: ManeuverUiData? = null,
    val tripProgress: TripProgressUiData? = null,
    val error: String? = null,
    val hasArrived: Boolean = false,
    val routePreview: List<NavigationRoute> = emptyList(),
)

data class ManeuverUiData(
    val primaryText: String,
    val secondaryText: String?,
    val distanceRemaining: String,
    val maneuverType: String?,
    val maneuverModifier: String?,
)

data class TripProgressUiData(
    val distanceRemaining: String,
    val timeRemaining: String,
    val estimatedArrival: String,
    val percentRouteTraveled: Float,
)

@HiltViewModel
class NavigationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val application: Application,
    private val navigationRepository: NavigationRepository,
    private val locationRepository: LocationRepository,
    private val tripProgressApi: MapboxTripProgressApi,
    private val speechApi: MapboxSpeechApi,
    private val voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer,
) : ViewModel() {

    private val workOrderId: String = savedStateHandle["workOrderId"] ?: ""
    private val destLat: Double = savedStateHandle.get<String>("destLat")?.toDoubleOrNull() ?: 0.0
    private val destLng: Double = savedStateHandle.get<String>("destLng")?.toDoubleOrNull() ?: 0.0

    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    val navigationRoutes: StateFlow<List<NavigationRoute>> = navigationRepository.navigationRoutes

    init {
        requestRoute()
        observeProgress()
        observeArrival()
        observeVoice()
    }

    private fun requestRoute() {
        viewModelScope.launch {
            val location = locationRepository.latestLocation.value
            if (location == null) {
                _uiState.update { it.copy(error = "nav_no_location") }
                return@launch
            }

            navigationRepository.requestRoute(
                originLat = location.latitude,
                originLng = location.longitude,
                destLat = destLat,
                destLng = destLng,
            ).collect { result ->
                when (result) {
                    is NavigationRouteResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is NavigationRouteResult.Success -> {
                        navigationRepository.setPreviewRoutes(result.routes)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                routePreview = result.routes,
                                error = null,
                            )
                        }
                    }
                    is NavigationRouteResult.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                    }
                }
            }
        }
    }

    fun startNavigation() {
        val routes = _uiState.value.routePreview
        if (routes.isEmpty()) return

        navigationRepository.startNavigation(routes)
        LocationTrackingService.navBoostInterval(application)
        _uiState.update { it.copy(isNavigating = true) }
    }

    fun stopNavigation() {
        navigationRepository.stopNavigation()
        LocationTrackingService.normalInterval(application)
        _uiState.update {
            it.copy(
                isNavigating = false,
                routePreview = emptyList(),
                currentManeuver = null,
                tripProgress = null,
            )
        }
    }

    private fun observeProgress() {
        viewModelScope.launch {
            navigationRepository.routeProgress.collect { progress ->
                if (progress == null) return@collect

                val maneuver = extractManeuver(progress)
                val trip = extractTripProgress(progress)

                _uiState.update {
                    it.copy(
                        currentManeuver = maneuver,
                        tripProgress = trip,
                    )
                }
            }
        }
    }

    private fun observeArrival() {
        viewModelScope.launch {
            navigationRepository.arrivalEvent.collect {
                _uiState.update { it.copy(hasArrived = true) }
                stopNavigation()
            }
        }
    }

    private fun observeVoice() {
        viewModelScope.launch {
            navigationRepository.voiceInstructions.collect { voiceInstruction ->
                speechApi.generate(voiceInstruction) { expected ->
                    expected.value?.let { speechValue ->
                        voiceInstructionsPlayer.play(speechValue) {
                            speechApi.clean(speechValue)
                        }
                    }
                }
            }
        }
    }

    private fun extractManeuver(progress: RouteProgress): ManeuverUiData? {
        val step = progress.currentLegProgress?.currentStepProgress?.step ?: return null
        val maneuver = step.maneuver() ?: return null
        val distanceRemaining = progress.currentLegProgress?.currentStepProgress
            ?.distanceRemaining?.let { formatDistance(it) } ?: ""

        return ManeuverUiData(
            primaryText = maneuver.instruction() ?: step.name() ?: "",
            secondaryText = null,
            distanceRemaining = distanceRemaining,
            maneuverType = maneuver.type(),
            maneuverModifier = maneuver.modifier(),
        )
    }

    private fun extractTripProgress(progress: RouteProgress): TripProgressUiData {
        val distanceRemaining = formatDistance(progress.distanceRemaining.toFloat())
        val durationRemaining = progress.durationRemaining
        val timeRemaining = formatDuration(durationRemaining)
        val eta = formatEta(durationRemaining)
        val traveled = if (progress.distanceTraveled + progress.distanceRemaining > 0) {
            (progress.distanceTraveled / (progress.distanceTraveled + progress.distanceRemaining)).toFloat()
        } else 0f

        return TripProgressUiData(
            distanceRemaining = distanceRemaining,
            timeRemaining = timeRemaining,
            estimatedArrival = eta,
            percentRouteTraveled = traveled,
        )
    }

    private fun formatDistance(meters: Float): String {
        return if (meters >= 1000) {
            String.format("%.1f km", meters / 1000)
        } else {
            String.format("%.0f m", meters)
        }
    }

    private fun formatDuration(seconds: Double): String {
        val mins = (seconds / 60).toInt()
        return if (mins >= 60) {
            val hours = mins / 60
            val remainingMins = mins % 60
            "${hours}h ${remainingMins}m"
        } else {
            "${mins} min"
        }
    }

    private fun formatEta(secondsRemaining: Double): String {
        val arrivalTime = System.currentTimeMillis() + (secondsRemaining * 1000).toLong()
        val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return formatter.format(java.util.Date(arrivalTime))
    }

    override fun onCleared() {
        stopNavigation()
        super.onCleared()
    }
}
