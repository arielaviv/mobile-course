package com.field.survey.ui.map.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.field.survey.domain.model.DistributionPoint
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle

@Composable
fun MapboxMapContent(
    distributionPoints: List<DistributionPoint>,
    mapboxToken: String,
    styleUri: String?,
) {
    if (mapboxToken.isBlank()) {
        Text(
            text = "Mapbox token not configured",
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    val viewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(34.7725, 32.0750))
            zoom(13.0)
        }
    }

    MapboxMap(
        modifier = Modifier.fillMaxSize(),
        mapViewportState = viewportState,
        style = { MapStyle(style = styleUri ?: Style.STANDARD) },
    ) {
        distributionPoints.forEach { dp ->
            val point = Point.fromLngLat(dp.longitude, dp.latitude)
            val color = dp.type.markerColor()

            CircleAnnotation(point = point) {
                circleRadius = 10.0
                circleColor = color
                circleStrokeWidth = 1.5
                circleStrokeColor = androidx.compose.ui.graphics.Color.White
            }
        }
    }
}
