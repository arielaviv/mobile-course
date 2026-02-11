package com.lux.field.ui.navigation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ManeuverIcon(
    type: String?,
    modifier: String?,
    iconModifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 36.dp,
) {
    Canvas(modifier = iconModifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        when {
            type == "arrive" -> drawArriveIcon(w, h, tint)
            type == "depart" -> drawStraightArrow(w, h, tint)
            type == "continue" || type == "new name" -> drawStraightArrow(w, h, tint)
            type == "roundabout" || type == "rotary" -> drawRoundaboutArrow(w, h, tint)
            isUturn(type, modifier) -> drawUturnArrow(w, h, tint, isLeft = modifier?.contains("left") != false)
            isSharpTurn(modifier) -> drawSharpTurnArrow(w, h, tint, isLeft = modifier?.contains("left") == true)
            isTurn(type, modifier) -> drawTurnArrow(w, h, tint, isLeft = modifier?.contains("left") == true)
            isSlightTurn(modifier) -> drawSlightArrow(w, h, tint, isLeft = modifier?.contains("left") == true)
            else -> drawStraightArrow(w, h, tint)
        }
    }
}

// ---- Arrow type detection ----

private fun isUturn(type: String?, modifier: String?): Boolean =
    type == "turn" && modifier?.contains("uturn") == true

private fun isSharpTurn(modifier: String?): Boolean =
    modifier?.contains("sharp") == true

private fun isTurn(type: String?, modifier: String?): Boolean =
    type == "turn" || type == "fork" || type == "on ramp" || type == "off ramp" ||
        modifier == "left" || modifier == "right"

private fun isSlightTurn(modifier: String?): Boolean =
    modifier?.contains("slight") == true

// ---- Drawing functions ----
// All paths drawn in a coordinate space where (0,0) is top-left, (w,h) is bottom-right.
// Arrow shapes are filled solid for maximum clarity at small sizes.

private fun DrawScope.drawStraightArrow(w: Float, h: Float, tint: Color) {
    val path = Path().apply {
        // Arrowhead
        moveTo(w * 0.50f, h * 0.08f)  // tip
        lineTo(w * 0.76f, h * 0.36f)  // right wing
        lineTo(w * 0.60f, h * 0.36f)  // inner right
        // Stem
        lineTo(w * 0.60f, h * 0.92f)  // bottom right
        lineTo(w * 0.40f, h * 0.92f)  // bottom left
        lineTo(w * 0.40f, h * 0.36f)  // inner left
        // Arrowhead
        lineTo(w * 0.24f, h * 0.36f)  // left wing
        close()
    }
    drawPath(path, color = tint)
}

private fun DrawScope.drawTurnArrow(w: Float, h: Float, tint: Color, isLeft: Boolean) {
    val path = Path().apply {
        if (isLeft) {
            // Arrow stem goes up from bottom, curves left, arrowhead points left
            moveTo(w * 0.58f, h * 0.92f)  // bottom right of stem
            lineTo(w * 0.58f, h * 0.48f)  // up right side
            cubicTo(w * 0.58f, h * 0.28f, w * 0.48f, h * 0.20f, w * 0.34f, h * 0.20f) // inner curve
            lineTo(w * 0.34f, h * 0.34f)  // arrowhead bottom wing
            lineTo(w * 0.08f, h * 0.17f)  // arrowhead tip (left)
            lineTo(w * 0.34f, h * 0.00f)  // arrowhead top wing
            lineTo(w * 0.34f, h * 0.10f)  // inner approach
            cubicTo(w * 0.56f, h * 0.10f, w * 0.70f, h * 0.20f, w * 0.70f, h * 0.48f) // outer curve
            lineTo(w * 0.70f, h * 0.92f)  // down left side (mirrored as right of stem)
            close()
        } else {
            // Arrow stem goes up from bottom, curves right, arrowhead points right
            moveTo(w * 0.30f, h * 0.92f)  // bottom left of stem
            lineTo(w * 0.30f, h * 0.48f)  // up left side
            cubicTo(w * 0.30f, h * 0.20f, w * 0.44f, h * 0.10f, w * 0.66f, h * 0.10f) // outer curve
            lineTo(w * 0.66f, h * 0.00f)  // arrowhead top wing
            lineTo(w * 0.92f, h * 0.17f)  // arrowhead tip (right)
            lineTo(w * 0.66f, h * 0.34f)  // arrowhead bottom wing
            lineTo(w * 0.66f, h * 0.20f)  // inner approach
            cubicTo(w * 0.52f, h * 0.20f, w * 0.42f, h * 0.28f, w * 0.42f, h * 0.48f) // inner curve
            lineTo(w * 0.42f, h * 0.92f)  // down right side of stem
            close()
        }
    }
    drawPath(path, color = tint)
}

private fun DrawScope.drawSlightArrow(w: Float, h: Float, tint: Color, isLeft: Boolean) {
    // Straight arrow angled slightly to the left or right (~30°)
    val path = Path().apply {
        if (isLeft) {
            moveTo(w * 0.22f, h * 0.10f)  // tip
            lineTo(w * 0.48f, h * 0.20f)  // right wing
            lineTo(w * 0.38f, h * 0.30f)  // inner right
            lineTo(w * 0.66f, h * 0.88f)  // bottom right
            lineTo(w * 0.50f, h * 0.92f)  // bottom left
            lineTo(w * 0.26f, h * 0.36f)  // inner left
            lineTo(w * 0.16f, h * 0.34f)  // left wing
            close()
        } else {
            moveTo(w * 0.78f, h * 0.10f)  // tip
            lineTo(w * 0.84f, h * 0.34f)  // right wing
            lineTo(w * 0.74f, h * 0.36f)  // inner right
            lineTo(w * 0.50f, h * 0.92f)  // bottom right
            lineTo(w * 0.34f, h * 0.88f)  // bottom left
            lineTo(w * 0.62f, h * 0.30f)  // inner left
            lineTo(w * 0.52f, h * 0.20f)  // left wing
            close()
        }
    }
    drawPath(path, color = tint)
}

private fun DrawScope.drawSharpTurnArrow(w: Float, h: Float, tint: Color, isLeft: Boolean) {
    // Arrow goes up then bends sharply back (135°)
    val path = Path().apply {
        if (isLeft) {
            moveTo(w * 0.58f, h * 0.92f)
            lineTo(w * 0.58f, h * 0.42f)
            lineTo(w * 0.30f, h * 0.62f)
            lineTo(w * 0.30f, h * 0.46f)  // arrowhead right wing
            lineTo(w * 0.08f, h * 0.72f)  // arrowhead tip
            lineTo(w * 0.14f, h * 0.44f)  // arrowhead left wing
            lineTo(w * 0.22f, h * 0.42f)
            lineTo(w * 0.70f, h * 0.20f)
            lineTo(w * 0.70f, h * 0.92f)
            close()
        } else {
            moveTo(w * 0.30f, h * 0.92f)
            lineTo(w * 0.30f, h * 0.20f)
            lineTo(w * 0.78f, h * 0.42f)
            lineTo(w * 0.86f, h * 0.44f)  // arrowhead right wing
            lineTo(w * 0.92f, h * 0.72f)  // arrowhead tip
            lineTo(w * 0.70f, h * 0.46f)  // arrowhead left wing
            lineTo(w * 0.70f, h * 0.62f)
            lineTo(w * 0.42f, h * 0.42f)
            lineTo(w * 0.42f, h * 0.92f)
            close()
        }
    }
    drawPath(path, color = tint)
}

private fun DrawScope.drawUturnArrow(w: Float, h: Float, tint: Color, isLeft: Boolean) {
    val path = Path().apply {
        if (isLeft) {
            // Left U-turn: goes up on right, curves left, comes back down on left
            moveTo(w * 0.20f, h * 0.60f)  // arrowhead bottom wing
            lineTo(w * 0.20f, h * 0.42f)  // arrowhead top wing
            lineTo(w * 0.08f, h * 0.56f)  // arrowhead tip (pointing down-left)
            // The above is simplified — let me redraw
            moveTo(w * 0.58f, h * 0.92f)  // bottom right of right stem
            lineTo(w * 0.58f, h * 0.32f)  // up right side
            cubicTo(w * 0.58f, h * 0.14f, w * 0.50f, h * 0.08f, w * 0.38f, h * 0.08f) // outer curve top
            cubicTo(w * 0.26f, h * 0.08f, w * 0.18f, h * 0.14f, w * 0.18f, h * 0.32f) // outer curve coming back
            lineTo(w * 0.18f, h * 0.46f)  // down to arrowhead area
            lineTo(w * 0.06f, h * 0.46f)  // arrowhead left wing
            lineTo(w * 0.24f, h * 0.70f)  // arrowhead tip (pointing down)
            lineTo(w * 0.42f, h * 0.46f)  // arrowhead right wing
            lineTo(w * 0.30f, h * 0.46f)  // inner approach
            lineTo(w * 0.30f, h * 0.32f)  // up inner left
            cubicTo(w * 0.30f, h * 0.22f, w * 0.34f, h * 0.18f, w * 0.38f, h * 0.18f) // inner curve top
            cubicTo(w * 0.42f, h * 0.18f, w * 0.46f, h * 0.22f, w * 0.46f, h * 0.32f) // inner curve right
            lineTo(w * 0.46f, h * 0.92f)  // down inner right
            close()
        } else {
            // Right U-turn: goes up on left, curves right, comes back down on right
            moveTo(w * 0.42f, h * 0.92f)
            lineTo(w * 0.42f, h * 0.32f)
            cubicTo(w * 0.42f, h * 0.14f, w * 0.50f, h * 0.08f, w * 0.62f, h * 0.08f)
            cubicTo(w * 0.74f, h * 0.08f, w * 0.82f, h * 0.14f, w * 0.82f, h * 0.32f)
            lineTo(w * 0.82f, h * 0.46f)
            lineTo(w * 0.94f, h * 0.46f)
            lineTo(w * 0.76f, h * 0.70f)
            lineTo(w * 0.58f, h * 0.46f)
            lineTo(w * 0.70f, h * 0.46f)
            lineTo(w * 0.70f, h * 0.32f)
            cubicTo(w * 0.70f, h * 0.22f, w * 0.66f, h * 0.18f, w * 0.62f, h * 0.18f)
            cubicTo(w * 0.58f, h * 0.18f, w * 0.54f, h * 0.22f, w * 0.54f, h * 0.32f)
            lineTo(w * 0.54f, h * 0.92f)
            close()
        }
    }
    drawPath(path, color = tint)
}

private fun DrawScope.drawRoundaboutArrow(w: Float, h: Float, tint: Color) {
    // Circular arrow with a stem coming from bottom
    val strokeWidth = w * 0.12f

    // Stem from bottom to circle
    drawLine(
        color = tint,
        start = Offset(w * 0.50f, h * 0.92f),
        end = Offset(w * 0.50f, h * 0.62f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )

    // Circular arc (3/4 of a circle)
    drawArc(
        color = tint,
        startAngle = 90f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(w * 0.20f, h * 0.10f),
        size = androidx.compose.ui.geometry.Size(w * 0.60f, h * 0.52f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )

    // Arrowhead at the end of the arc (pointing down at right side)
    val arrowPath = Path().apply {
        moveTo(w * 0.50f, h * 0.60f)  // tip
        lineTo(w * 0.62f, h * 0.46f)
        lineTo(w * 0.38f, h * 0.46f)
        close()
    }
    drawPath(arrowPath, color = tint)
}

private fun DrawScope.drawArriveIcon(w: Float, h: Float, tint: Color) {
    // Destination pin / filled circle with inner dot
    val centerX = w * 0.50f
    val centerY = h * 0.40f
    val outerRadius = w * 0.30f
    val innerRadius = w * 0.12f

    // Outer circle
    drawCircle(
        color = tint,
        radius = outerRadius,
        center = Offset(centerX, centerY),
    )

    // Inner circle (cutout look)
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = innerRadius,
        center = Offset(centerX, centerY),
    )

    // Pin point triangle
    val pinPath = Path().apply {
        moveTo(centerX - w * 0.14f, centerY + h * 0.14f)
        lineTo(centerX, h * 0.82f)
        lineTo(centerX + w * 0.14f, centerY + h * 0.14f)
        close()
    }
    drawPath(pinPath, color = tint)
}
