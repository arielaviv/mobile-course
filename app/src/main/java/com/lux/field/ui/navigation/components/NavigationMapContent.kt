package com.lux.field.ui.navigation.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.DisposableMapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources

@Composable
fun NavigationMapContent(
    routes: List<NavigationRoute>,
    destLat: Double,
    destLng: Double,
    isNavigating: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density

    // ---- Remembered navigation components (survive recomposition) ----

    val navigationLocationProvider = remember { NavigationLocationProvider() }

    val routeLineApi = remember {
        MapboxRouteLineApi(
            MapboxRouteLineApiOptions.Builder()
                .vanishingRouteLineEnabled(true)
                .build()
        )
    }

    val routeLineView = remember {
        MapboxRouteLineView(
            MapboxRouteLineViewOptions.Builder(context)
                .routeLineColorResources(
                    RouteLineColorResources.Builder()
                        .routeDefaultColor(AndroidColor.parseColor("#3B82F6"))
                        .routeCasingColor(AndroidColor.parseColor("#1D4ED8"))
                        .alternativeRouteDefaultColor(AndroidColor.parseColor("#52525B"))
                        .alternativeRouteCasingColor(AndroidColor.parseColor("#3F3F46"))
                        .build()
                )
                .displaySoftGradientForTraffic(true)
                .softGradientTransition(30.0)
                .build()
        )
    }

    val routeArrowApi = remember { MapboxRouteArrowApi() }
    val routeArrowView = remember {
        MapboxRouteArrowView(
            RouteArrowOptions.Builder(context)
                .withArrowColor(AndroidColor.parseColor("#93C5FD"))
                .withArrowBorderColor(AndroidColor.parseColor("#1D4ED8"))
                .build()
        )
    }

    // Hoisted camera references — set inside DisposableMapEffect, used by LaunchedEffect
    var navigationCamera by remember { mutableStateOf<NavigationCamera?>(null) }
    var viewportDataSource by remember { mutableStateOf<MapboxNavigationViewportDataSource?>(null) }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(destLng, destLat))
            zoom(14.0)
            pitch(0.0)
        }
    }

    // ---- Camera state transitions ----
    LaunchedEffect(isNavigating, navigationCamera) {
        val camera = navigationCamera ?: return@LaunchedEffect
        if (isNavigating) {
            camera.requestNavigationCameraToFollowing()
        }
    }

    // When routes arrive during preview, show overview
    LaunchedEffect(routes, navigationCamera) {
        val camera = navigationCamera ?: return@LaunchedEffect
        if (routes.isNotEmpty() && !isNavigating) {
            camera.requestNavigationCameraToOverview()
        }
    }

    MapboxMap(
        modifier = modifier.fillMaxSize(),
        mapViewportState = mapViewportState,
        style = { MapStyle(style = Style.STANDARD) },
    ) {
        // Destination marker — bright blue circle with white border
        CircleAnnotation(
            point = Point.fromLngLat(destLng, destLat),
        ) {
            circleRadius = 12.0
            circleColor = Color(0xFF3B82F6)
            circleStrokeWidth = 3.0
            circleStrokeColor = Color.White
        }

        // ---- Wire up puck, camera, route line, route arrow ----
        DisposableMapEffect(Unit) { mapView ->
            val mapboxMap = mapView.mapboxMap

            // --- 1. Location Puck ---
            mapView.location.apply {
                setLocationProvider(navigationLocationProvider)
                locationPuck = createDefault2DPuck(withBearing = true)
                puckBearingEnabled = true
                puckBearing = PuckBearing.COURSE
                enabled = true
            }

            // --- 2. Navigation Camera + Viewport Data Source ---
            val vds = MapboxNavigationViewportDataSource(mapboxMap)
            vds.followingPadding = EdgeInsets(
                180.0 * density,  // top padding for maneuver banner
                40.0 * density,
                150.0 * density,  // bottom padding for trip progress bar
                40.0 * density,
            )
            vds.overviewPadding = EdgeInsets(
                140.0 * density,
                40.0 * density,
                120.0 * density,
                40.0 * density,
            )
            viewportDataSource = vds

            val camera = NavigationCamera(
                mapboxMap,
                mapView.camera,
                vds,
            )
            navigationCamera = camera

            // --- 3. Observers on MapboxNavigation ---
            val navigation = MapboxNavigationApp.current()

            // Location → feeds puck animation + camera position
            val locationObserver = object : LocationObserver {
                override fun onNewRawLocation(rawLocation: android.location.Location) {
                    // Raw location available even without trip session —
                    // seed puck so it shows during route preview
                    navigationLocationProvider.changePosition(rawLocation, emptyList())
                    vds.onLocationChanged(rawLocation)
                    vds.evaluate()
                }

                override fun onNewLocationMatcherResult(
                    locationMatcherResult: LocationMatcherResult,
                ) {
                    // Enhanced road-snapped location during active navigation
                    navigationLocationProvider.changePosition(
                        locationMatcherResult.enhancedLocation,
                        locationMatcherResult.keyPoints,
                    )
                    vds.onLocationChanged(locationMatcherResult.enhancedLocation)
                    vds.evaluate()
                }
            }

            // Routes → renders route line + updates camera framing
            val routesObserver = RoutesObserver { result ->
                val altMetadata = navigation?.getAlternativeMetadataFor(
                    result.navigationRoutes
                ) ?: emptyList()

                routeLineApi.setNavigationRoutes(
                    result.navigationRoutes,
                    altMetadata,
                ) { drawData ->
                    mapboxMap.style?.let { style ->
                        routeLineView.renderRouteDrawData(style, drawData)
                    }
                }

                if (result.navigationRoutes.isNotEmpty()) {
                    vds.onRouteChanged(result.navigationRoutes.first())
                } else {
                    vds.clearRouteData()
                }
                vds.evaluate()
            }

            // Progress → vanishing route line + route arrow + camera
            val progressObserver = RouteProgressObserver { progress ->
                // Vanishing route line behind user
                routeLineApi.updateWithRouteProgress(progress) { result ->
                    mapboxMap.style?.let { style ->
                        routeLineView.renderRouteLineUpdate(style, result)
                    }
                }

                // Maneuver arrow on map at next turn
                routeArrowApi.addUpcomingManeuverArrow(progress).let { arrowResult ->
                    mapboxMap.style?.let { style ->
                        routeArrowView.renderManeuverUpdate(style, arrowResult)
                    }
                }

                // Camera follows progress
                vds.onRouteProgressChanged(progress)
                vds.evaluate()
            }

            // Position change → keeps vanishing trail smooth
            val positionListener = OnIndicatorPositionChangedListener { point ->
                routeLineApi.updateTraveledRouteLine(point).let { result ->
                    mapboxMap.style?.let { style ->
                        routeLineView.renderRouteLineUpdate(style, result)
                    }
                }
            }

            // Register all observers
            navigation?.registerLocationObserver(locationObserver)
            navigation?.registerRoutesObserver(routesObserver)
            navigation?.registerRouteProgressObserver(progressObserver)
            mapView.location.addOnIndicatorPositionChangedListener(positionListener)

            onDispose {
                navigation?.unregisterLocationObserver(locationObserver)
                navigation?.unregisterRoutesObserver(routesObserver)
                navigation?.unregisterRouteProgressObserver(progressObserver)
                mapView.location.removeOnIndicatorPositionChangedListener(positionListener)
                routeLineApi.cancel()
                routeLineView.cancel()
            }
        }
    }
}
