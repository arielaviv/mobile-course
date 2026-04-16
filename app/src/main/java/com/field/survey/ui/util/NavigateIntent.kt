package com.field.survey.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.field.survey.R

object NavigateIntent {

    /**
     * Fires a geo: intent via a chooser so the user picks Waze / Google Maps / Maps.
     * Falls back to the Google Maps web URL if no activity resolves.
     */
    fun launch(
        context: Context,
        latitude: Double,
        longitude: Double,
        label: String,
    ) {
        val encoded = Uri.encode(label.ifBlank { context.getString(R.string.navigate_destination_fallback) })
        val geoUri = Uri.parse("geo:0,0?q=$latitude,$longitude($encoded)")
        val intent = Intent(Intent.ACTION_VIEW, geoUri)
        val resolved = intent.resolveActivity(context.packageManager) != null
        if (resolved) {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.navigate_chooser)))
        } else {
            val web = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            context.startActivity(Intent(Intent.ACTION_VIEW, web))
        }
    }

    fun share(
        context: Context,
        latitude: Double,
        longitude: Double,
        label: String,
    ) {
        val text =
            buildString {
                if (label.isNotBlank()) {
                    append(label)
                    append('\n')
                }
                append("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            }
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_location_chooser)))
    }
}
