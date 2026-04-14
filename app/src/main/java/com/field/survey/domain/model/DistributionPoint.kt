package com.field.survey.domain.model

import androidx.annotation.StringRes
import com.field.survey.R

enum class DpType(
    @StringRes val labelRes: Int,
    val isLine: Boolean,
) {
    UNDERGROUND_PATH(R.string.dp_type_underground_path, true),
    AERIAL_SPAN(R.string.dp_type_aerial_span, true),
    POLES(R.string.dp_type_poles, false),
    CENTRAL_OFFICES(R.string.dp_type_central_offices, false),
}

fun String.toDpType(): DpType =
    when (this) {
        "DUCT", "UNDERGROUND_PATH" -> DpType.UNDERGROUND_PATH
        "AERIAL_SPAN" -> DpType.AERIAL_SPAN
        "POLE", "POLES" -> DpType.POLES
        "CABINET", "CENTRAL_OFFICES" -> DpType.CENTRAL_OFFICES
        else -> runCatching { DpType.valueOf(this) }.getOrDefault(DpType.POLES)
    }

data class DistributionPoint(
    val id: String,
    val label: String,
    val type: DpType,
    val latitude: Double,
    val longitude: Double,
    val pathCoordinates: String?,
    val photoPath: String?,
    val imageBase64: String?,
    val notes: String,
    val createdAt: Long,
    val createdBy: String,
    val createdByName: String,
    val updatedAt: Long? = null,
    val updatedBy: String? = null,
)
