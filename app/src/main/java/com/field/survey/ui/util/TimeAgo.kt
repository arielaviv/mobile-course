package com.field.survey.ui.util

import kotlin.math.max

object TimeAgo {

    fun format(
        timestampMs: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): String {
        val diff = max(0L, nowMs - timestampMs)
        val seconds = diff / 1_000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        return when {
            seconds < 60 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            days < 30 -> "${days / 7}w ago"
            days < 365 -> "${days / 30}mo ago"
            else -> "${days / 365}y ago"
        }
    }
}
