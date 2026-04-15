package com.field.survey.ui.stats

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.field.survey.R

class PieChartView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
    ) : View(context, attrs, defStyle) {

        data class Slice(
            val color: Int,
            val fraction: Float,
        )

        private var slices: List<Slice> = emptyList()
        private var animProgress = 1f
        private var animator: ValueAnimator? = null

        private val strokeWidthPx = 22f * resources.displayMetrics.density

        private val arcPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeWidthPx
                strokeCap = Paint.Cap.BUTT
            }

        private val emptyPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeWidthPx
                color = ContextCompat.getColor(context, R.color.m3_surface_container_high)
            }

        private val bounds = RectF()

        fun setData(
            slices: List<Slice>,
            animate: Boolean = true,
        ) {
            this.slices = slices
            animator?.cancel()
            if (animate) {
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
            } else {
                animator = null
                animProgress = 1f
                invalidate()
            }
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            animator?.cancel()
            animator = null
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val inset = strokeWidthPx / 2f + 4f
            val size = minOf(width, height).toFloat()
            val cx = width / 2f
            val cy = height / 2f
            bounds.set(cx - size / 2f + inset, cy - size / 2f + inset, cx + size / 2f - inset, cy + size / 2f - inset)

            canvas.drawArc(bounds, 0f, 360f, false, emptyPaint)

            if (slices.isEmpty()) return

            var startAngle = -90f
            val gap = 1.5f
            for (slice in slices) {
                val sweep = slice.fraction * 360f * animProgress
                if (sweep > gap) {
                    arcPaint.color = slice.color
                    canvas.drawArc(bounds, startAngle, sweep - gap, false, arcPaint)
                }
                startAngle += sweep
            }
        }
    }
