package com.field.survey.domain.model

import androidx.annotation.StringRes
import com.field.survey.R

enum class DpType(@StringRes val labelRes: Int) {
    MANHOLE(R.string.dp_type_manhole),
    JUNCTION_BOX(R.string.dp_type_junction_box),
    CABINET(R.string.dp_type_cabinet),
    POLE(R.string.dp_type_pole),
    DUCT(R.string.dp_type_duct),
    HANDHOLE(R.string.dp_type_handhole),
    PEDESTAL(R.string.dp_type_pedestal),
    OTHER(R.string.dp_type_other),
}

fun String.toDpType(): DpType = when (this) {
    "DP" -> DpType.CABINET
    "JB" -> DpType.JUNCTION_BOX
    "MH" -> DpType.MANHOLE
    "SPLICE_CLOSURE" -> DpType.OTHER
    else -> try { DpType.valueOf(this) } catch (_: Exception) { DpType.OTHER }
}

data class DistributionPoint(
    val id: String,
    val label: String,
    val type: DpType,
    val latitude: Double,
    val longitude: Double,
    val photoPath: String?,
    val imageBase64: String?,
    val notes: String,
    val createdAt: Long,
    val createdBy: String,
    val createdByName: String,
)
