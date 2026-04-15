package com.field.survey.ui.stats

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.field.survey.R

class BarChartView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
    ) : View(context, attrs, defStyle) {

        data class Bar(
            val value: Int,
            val label: String,
        )

        private var bars: List<Bar> = emptyList()
        private var maxValue = 1
        private var animProgress = 1f
        private var animator: ValueAnimator? = null

        private val density = resources.displayMetrics.density
        private val valueSpace = 16f * density
        private val labelSpace = 18f * density
        private val corner = 6f * density

        private val primaryColor: Int = resolveAttr(com.google.android.material.R.attr.colorPrimary)
        private val onSurfaceColor: Int = ContextCompat.getColor(context, R.color.m3_on_surface)
        private val onSurfaceVariantColor: Int = ContextCompat.getColor(context, R.color.m3_on_surface_variant)

        private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val valuePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = 11f * density
                color = onSurfaceColor
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

        private val labelPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = 10f * density
                color = onSurfaceVariantColor
            }

        fun setData(bars: List<Bar>) {
            this.bars = bars
            maxValue = maxOf(1, bars.maxOfOrNull { it.value } ?: 1)
            animator?.cancel()
            animator =
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 700
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener {
                        animProgress = it.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            animator?.cancel()
            animator = null
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (bars.isEmpty()) return

            val chartTop = valueSpace
            val chartBottom = height - labelSpace
            val chartHeight = chartBottom - chartTop

            val barWidth = width.toFloat() / bars.size
            val actualBarWidth = barWidth * 0.55f
            val offset = (barWidth - actualBarWidth) / 2f

            bars.forEachIndexed { i, bar ->
                val x = i * barWidth + offset
                val targetHeight = (bar.value.toFloat() / maxValue) * chartHeight
                val animatedHeight = targetHeight * animProgress
                val topY = chartBottom - animatedHeight

                barPaint.color =
                    if (bar.value == 0) {
                        Color.argb(40, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
                    } else {
                        primaryColor
                    }
                canvas.drawRoundRect(x, topY, x + actualBarWidth, chartBottom, corner, corner, barPaint)

                if (bar.value > 0 && animProgress > 0.5f) {
                    val valueAlpha = ((animProgress - 0.5f) * 2f).coerceIn(0f, 1f)
                    valuePaint.alpha = (valueAlpha * 255).toInt()
                    canvas.drawText(
                        bar.value.toString(),
                        x + actualBarWidth / 2f,
                        topY - 4f * density,
                        valuePaint,
                    )
                }

                canvas.drawText(
                    bar.label,
                    x + actualBarWidth / 2f,
                    chartBottom + labelSpace - 5f * density,
                    labelPaint,
                )
            }
        }

        private fun resolveAttr(attr: Int): Int {
            val tv = TypedValue()
            return if (context.theme.resolveAttribute(attr, tv, true)) tv.data else Color.BLUE
        }
    }
