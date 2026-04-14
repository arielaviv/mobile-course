package com.field.survey.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.field.survey.R

/**
 * Density-aware marker bitmap factory. Produces composited backplate + tinted icon bitmaps
 * sized in pixels (not dp) so Mapbox renders them at their intended visual size on all densities.
 *
 * Cache lives at fragment scope; call [build] once per lifecycle and pass [MarkerSet] into the map.
 */
object MarkerBitmaps {

    private const val BACKPLATE_WIDTH_DP = 48f
    private const val BACKPLATE_HEIGHT_DP = 56f
    private const val ICON_WIDTH_DP = 24f
    private const val ICON_PADDING_TOP_DP = 10f

    data class MarkerSet(
        val pole: Bitmap,
        val centralOffice: Bitmap,
    )

    fun build(context: Context): MarkerSet {
        val density = context.resources.displayMetrics.density
        val poleColor = ContextCompat.getColor(context, R.color.type_poles)
        val officeColor = ContextCompat.getColor(context, R.color.type_central_offices)
        return MarkerSet(
            pole = compose(context, R.drawable.ic_utility_pole, poleColor, density),
            centralOffice = compose(context, R.drawable.ic_house_plug, officeColor, density),
        )
    }

    private fun compose(
        context: Context,
        @DrawableRes iconRes: Int,
        @ColorInt tint: Int,
        density: Float,
    ): Bitmap {
        val widthPx = (BACKPLATE_WIDTH_DP * density).toInt()
        val heightPx = (BACKPLATE_HEIGHT_DP * density).toInt()
        val iconPx = (ICON_WIDTH_DP * density).toInt()
        val iconTopPx = (ICON_PADDING_TOP_DP * density).toInt()
        val iconLeftPx = (widthPx - iconPx) / 2

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val backplate = ResourcesCompat.getDrawable(context.resources, R.drawable.marker_backplate, context.theme)!!
        backplate.setBounds(0, 0, widthPx, heightPx)
        backplate.draw(canvas)

        val icon = ResourcesCompat.getDrawable(context.resources, iconRes, context.theme)!!.mutate()
        icon.colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN)
        icon.setBounds(iconLeftPx, iconTopPx, iconLeftPx + iconPx, iconTopPx + iconPx)
        icon.draw(canvas)

        return bitmap
    }
}
