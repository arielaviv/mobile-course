package com.lux.field.ui.map.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.lux.field.domain.model.WorkOrder
import com.lux.field.domain.model.WorkOrderStatus
import com.lux.field.ui.theme.StatusBlocked
import com.lux.field.ui.theme.StatusCompleted
import com.lux.field.ui.theme.StatusDraft
import com.lux.field.ui.theme.StatusFailed
import com.lux.field.ui.theme.StatusInProgress
import com.lux.field.ui.theme.StatusPending
import com.lux.field.ui.theme.StatusScheduled
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun OsmMapContent(
    workOrders: List<WorkOrder>,
    selectedWorkOrder: WorkOrder?,
    onMarkerClick: (WorkOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(32.0750, 34.7725))
        }
    }

    LaunchedEffect(workOrders, selectedWorkOrder) {
        mapView.overlays.clear()

        workOrders.forEach { wo ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(wo.location.latitude, wo.location.longitude)
                title = wo.title
                snippet = wo.location.address
                icon = createMarkerDrawable(
                    color = wo.status.toMarkerColor(),
                    isSelected = wo.id == selectedWorkOrder?.id,
                )
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { _, _ ->
                    onMarkerClick(wo)
                    true
                }
            }
            mapView.overlays.add(marker)
        }

        if (workOrders.size >= 2) {
            val lats = workOrders.map { it.location.latitude }
            val lngs = workOrders.map { it.location.longitude }
            val box = BoundingBox(
                lats.max() + 0.005,
                lngs.max() + 0.005,
                lats.min() - 0.005,
                lngs.min() - 0.005,
            )
            mapView.post { mapView.zoomToBoundingBox(box, true, 100) }
        }

        mapView.invalidate()
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}

private fun createMarkerDrawable(color: Color, isSelected: Boolean): Drawable {
    val size = if (isSelected) 48 else 36
    val strokeWidth = if (isSelected) 6 else 3

    val fill = ShapeDrawable(OvalShape()).apply {
        intrinsicWidth = size
        intrinsicHeight = size
        paint.color = color.toArgb()
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
    }

    val stroke = object : ShapeDrawable(OvalShape()) {
        override fun draw(canvas: Canvas) {
            paint.color = 0xFFFFFFFF.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth.toFloat()
            paint.isAntiAlias = true
            val cx = bounds.exactCenterX()
            val cy = bounds.exactCenterY()
            val radius = (bounds.width() / 2f) - (strokeWidth / 2f)
            canvas.drawCircle(cx, cy, radius, paint)
        }
    }.apply {
        intrinsicWidth = size
        intrinsicHeight = size
    }

    return LayerDrawable(arrayOf(fill, stroke)).apply {
        setBounds(0, 0, size, size)
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
