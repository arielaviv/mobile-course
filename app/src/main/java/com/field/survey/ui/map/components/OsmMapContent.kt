package com.field.survey.ui.map.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.field.survey.domain.model.DistributionPoint
import com.field.survey.domain.model.DpType
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun OsmMapContent(
    distributionPoints: List<DistributionPoint>,
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

    LaunchedEffect(distributionPoints) {
        mapView.overlays.clear()

        distributionPoints.forEach { dp ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(dp.latitude, dp.longitude)
                title = "${dp.type.name}: ${dp.label}"
                snippet = dp.notes.ifBlank { null }
                icon = createSquareMarkerDrawable(color = dp.type.markerColor())
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
            mapView.overlays.add(marker)
        }

        val allLats = distributionPoints.map { it.latitude }
        val allLngs = distributionPoints.map { it.longitude }
        if (allLats.size >= 2) {
            val box = BoundingBox(
                allLats.max() + 0.005,
                allLngs.max() + 0.005,
                allLats.min() - 0.005,
                allLngs.min() - 0.005,
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

private fun createSquareMarkerDrawable(color: Color): Drawable {
    val size = 32
    val strokeWidth = 3

    val fill = ShapeDrawable(RectShape()).apply {
        intrinsicWidth = size
        intrinsicHeight = size
        paint.color = color.toArgb()
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
    }

    val stroke = object : ShapeDrawable(RectShape()) {
        override fun draw(canvas: Canvas) {
            paint.color = 0xFFFFFFFF.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth.toFloat()
            paint.isAntiAlias = true
            canvas.drawRect(bounds, paint)
        }
    }.apply {
        intrinsicWidth = size
        intrinsicHeight = size
    }

    return LayerDrawable(arrayOf(fill, stroke)).apply {
        setBounds(0, 0, size, size)
    }
}

internal fun DpType.markerColor(): Color = when (this) {
    DpType.MANHOLE -> Color(0xFF6366F1)
    DpType.JUNCTION_BOX -> Color(0xFF3B82F6)
    DpType.CABINET -> Color(0xFF10B981)
    DpType.POLE -> Color(0xFFF59E0B)
    DpType.DUCT -> Color(0xFF8B5CF6)
    DpType.HANDHOLE -> Color(0xFF06B6D4)
    DpType.PEDESTAL -> Color(0xFFF97316)
    DpType.OTHER -> Color(0xFF71717A)
}
