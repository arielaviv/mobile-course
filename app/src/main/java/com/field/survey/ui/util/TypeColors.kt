package com.field.survey.ui.util

import androidx.annotation.ColorRes
import com.field.survey.R
import com.field.survey.domain.model.DpType

@ColorRes
fun DpType.colorRes(): Int =
    when (this) {
        DpType.UNDERGROUND_PATH -> R.color.type_underground_path
        DpType.AERIAL_SPAN -> R.color.type_aerial_span
        DpType.POLES -> R.color.type_poles
        DpType.CENTRAL_OFFICES -> R.color.type_central_offices
    }
