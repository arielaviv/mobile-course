package com.field.survey.ui.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoMath {

    private const val EARTH_RADIUS_M = 6_371_000.0

    fun haversineMeters(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a =
            sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    fun formatDistance(meters: Double): String {
        if (meters < 1_000.0) return "%.0f m".format(meters)
        return "%.1f km".format(meters / 1_000.0)
    }

    fun polylineMeters(verts: List<Pair<Double, Double>>): Double {
        if (verts.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until verts.size) {
            val (lng1, lat1) = verts[i - 1]
            val (lng2, lat2) = verts[i]
            total += haversineMeters(lat1, lng1, lat2, lng2)
        }
        return total
    }
}
