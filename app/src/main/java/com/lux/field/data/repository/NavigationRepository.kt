package com.lux.field.data.repository

import android.util.Log
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.routealternatives.NavigationRouteAlternativesObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface NavigationRouteResult {
    data class Success(val routes: List<NavigationRoute>) : NavigationRouteResult
    data class Error(val message: String) : NavigationRouteResult
    data object Loading : NavigationRouteResult
}

data class ArrivalEvent(val routeProgress: RouteProgress)

@Singleton
class NavigationRepository @Inject constructor() {

    private val _routeProgress = MutableStateFlow<RouteProgress?>(null)
    val routeProgress: StateFlow<RouteProgress?> = _routeProgress.asStateFlow()

    private val _navigationRoutes = MutableStateFlow<List<NavigationRoute>>(emptyList())
    val navigationRoutes: StateFlow<List<NavigationRoute>> = _navigationRoutes.asStateFlow()

    private val _arrivalEvent = MutableSharedFlow<ArrivalEvent>(extraBufferCapacity = 1)
    val arrivalEvent: Flow<ArrivalEvent> = _arrivalEvent.asSharedFlow()

    private val _voiceInstructions = MutableSharedFlow<com.mapbox.api.directions.v5.models.VoiceInstructions>(extraBufferCapacity = 1)
    val voiceInstructions: Flow<com.mapbox.api.directions.v5.models.VoiceInstructions> = _voiceInstructions.asSharedFlow()

    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    private val routeProgressObserver = RouteProgressObserver { progress ->
        _routeProgress.value = progress
    }

    private val routesObserver = RoutesObserver { result ->
        _navigationRoutes.value = result.navigationRoutes
    }

    private val arrivalObserver = object : ArrivalObserver {
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            _arrivalEvent.tryEmit(ArrivalEvent(routeProgress))
        }

        override fun onNextRouteLegStart(routeLegProgress: com.mapbox.navigation.base.trip.model.RouteLegProgress) {
            // multi-leg routes not used
        }

        override fun onWaypointArrival(routeProgress: RouteProgress) {
            // waypoints not used
        }
    }

    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        voiceInstructions.voiceInstructions?.let {
            _voiceInstructions.tryEmit(it)
        }
    }

    private fun getNavigation(): MapboxNavigation? = MapboxNavigationApp.current()

    fun requestRoute(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
    ): Flow<NavigationRouteResult> = callbackFlow {
        trySend(NavigationRouteResult.Loading)

        val navigation = getNavigation()
        if (navigation == null) {
            trySend(NavigationRouteResult.Error("Navigation not initialized"))
            close()
            return@callbackFlow
        }

        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(
                listOf(
                    Point.fromLngLat(originLng, originLat),
                    Point.fromLngLat(destLng, destLat),
                )
            )
            .alternatives(true)
            .build()

        navigation.requestRoutes(
            routeOptions,
            object : RoutesRequestCallback {
                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: RouterOrigin) {
                    if (routes.isNotEmpty()) {
                        trySend(NavigationRouteResult.Success(routes))
                    } else {
                        trySend(NavigationRouteResult.Error("No routes found"))
                    }
                    close()
                }

                override fun onRoutesRequestFailure(
                    throwable: Throwable,
                    routeOptions: RouteOptions,
                ) {
                    Log.e(TAG, "Route request failed", throwable)
                    trySend(NavigationRouteResult.Error(throwable.message ?: "Route request failed"))
                    close()
                }

                override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                    trySend(NavigationRouteResult.Error("Route request cancelled"))
                    close()
                }
            }
        )

        awaitClose()
    }

    fun startNavigation(routes: List<NavigationRoute>) {
        val navigation = getNavigation() ?: return
        navigation.setNavigationRoutes(routes)
        navigation.startTripSession()
        navigation.registerRouteProgressObserver(routeProgressObserver)
        navigation.registerRoutesObserver(routesObserver)
        navigation.registerArrivalObserver(arrivalObserver)
        navigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        _isNavigating.value = true
    }

    fun setPreviewRoutes(routes: List<NavigationRoute>) {
        val navigation = getNavigation() ?: return
        navigation.setNavigationRoutes(routes)
        navigation.registerRoutesObserver(routesObserver)
    }

    fun stopNavigation() {
        val navigation = getNavigation() ?: return
        try {
            navigation.unregisterRouteProgressObserver(routeProgressObserver)
            navigation.unregisterRoutesObserver(routesObserver)
            navigation.unregisterArrivalObserver(arrivalObserver)
            navigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
            navigation.stopTripSession()
            navigation.setNavigationRoutes(emptyList())
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping navigation", e)
        }
        _isNavigating.value = false
        _routeProgress.value = null
        _navigationRoutes.value = emptyList()
    }

    companion object {
        private const val TAG = "NavigationRepository"
    }
}
