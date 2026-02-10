package com.lux.field.ui.map.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.lux.field.domain.model.WorkOrder
import com.lux.field.domain.model.WorkOrderStatus
import com.lux.field.ui.theme.StatusBlocked
import com.lux.field.ui.theme.StatusCompleted
import com.lux.field.ui.theme.StatusDraft
import com.lux.field.ui.theme.StatusFailed
import com.lux.field.ui.theme.StatusInProgress
import com.lux.field.ui.theme.StatusPending
import com.lux.field.ui.theme.StatusScheduled
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle

@Composable
fun WorkOrderMapContent(
    workOrders: List<WorkOrder>,
    selectedWorkOrder: WorkOrder?,
    mapboxToken: String,
    styleUri: String?,
    onMarkerClick: (WorkOrder) -> Unit,
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

    LaunchedEffect(workOrders) {
        if (workOrders.size >= 2) {
            val lats = workOrders.map { it.location.latitude }
            val lngs = workOrders.map { it.location.longitude }
            val sw = Point.fromLngLat(lngs.min(), lats.min())
            val ne = Point.fromLngLat(lngs.max(), lats.max())
            viewportState.setCameraOptions {
                center(
                    Point.fromLngLat(
                        (sw.longitude() + ne.longitude()) / 2,
                        (sw.latitude() + ne.latitude()) / 2,
                    )
                )
                zoom(12.5)
            }
        } else if (workOrders.size == 1) {
            val wo = workOrders.first()
            viewportState.setCameraOptions {
                center(Point.fromLngLat(wo.location.longitude, wo.location.latitude))
                zoom(14.0)
            }
        }
    }

    MapboxMap(
        modifier = Modifier.fillMaxSize(),
        mapViewportState = viewportState,
        style = { MapStyle(style = styleUri ?: Style.STANDARD) },
    ) {
        workOrders.forEach { wo ->
            val point = Point.fromLngLat(wo.location.longitude, wo.location.latitude)
            val isSelected = selectedWorkOrder?.id == wo.id
            val markerColor = wo.status.toMarkerColor()

            CircleAnnotation(
                point = point,
            ) {
                circleRadius = if (isSelected) 14.0 else 10.0
                circleColor = markerColor
                circleStrokeWidth = if (isSelected) 3.0 else 1.5
                circleStrokeColor = Color.White
                interactionsState.onClicked {
                    onMarkerClick(wo)
                    true
                }
            }
        }
    }
}

private fun WorkOrderStatus.toMarkerColor(): Color = when (this) {
    WorkOrderStatus.DRAFT -> StatusDraft
    WorkOrderStatus.PENDING -> StatusPending
    WorkOrderStatus.SCHEDULED -> StatusScheduled
    WorkOrderStatus.IN_PROGRESS -> StatusInProgress
    WorkOrderStatus.COMPLETED -> StatusCompleted
    WorkOrderStatus.FAILED -> StatusFailed
    WorkOrderStatus.CANCELLED -> StatusBlocked
}
