package com.field.survey.ui.util

import android.content.Context
import com.field.survey.R
import kotlin.math.max

object TimeAgo {

    fun format(
        context: Context,
        timestampMs: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): String {
        val diff = max(0L, nowMs - timestampMs)
        val seconds = diff / 1_000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        return when {
            seconds < 60 -> context.getString(R.string.time_just_now)
            minutes < 60 -> context.getString(R.string.time_minutes_ago, minutes.toInt())
            hours < 24 -> context.getString(R.string.time_hours_ago, hours.toInt())
            days < 7 -> context.getString(R.string.time_days_ago, days.toInt())
            days < 30 -> context.getString(R.string.time_weeks_ago, (days / 7).toInt())
            days < 365 -> context.getString(R.string.time_months_ago, (days / 30).toInt())
            else -> context.getString(R.string.time_years_ago, (days / 365).toInt())
        }
    }
}
