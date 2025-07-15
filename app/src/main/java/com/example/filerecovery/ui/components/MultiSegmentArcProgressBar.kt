package com.example.filerecovery.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.filerecovery.R
import com.example.filerecovery.ui.components.MultiSegmentProgressBar.Segment
import kotlin.math.min

class MultiSegmentArcProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {


    private data class AnimatedSegment(val targetPercentage: Float, val color: Int)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var backgroundColor: Int = ContextCompat.getColor(context, R.color.progress_background)

    private val startAngle = 180f
    private val sweepAngleTotal = 180f
    private var strokeWidth = 60f

    private val segments = mutableListOf<AnimatedSegment>()
    private var animatedFractions = mutableListOf<Float>()

    fun setSegments(
        newSegments: List<Segment>,
        remainingColor: Int = ContextCompat.getColor(context, R.color.progress_background)
    ) {
        segments.clear()
        animatedFractions.clear()
        backgroundColor = remainingColor

        val totalPercent = newSegments.sumOf { it.percentage.toDouble() }.toFloat()
        newSegments.forEach {
            segments.add(AnimatedSegment(it.percentage, it.color))
            animatedFractions.add(0f)
        }

        val remaining = 100f - totalPercent
        if (remaining > 0f) {
            segments.add(AnimatedSegment(remaining, backgroundColor))
            animatedFractions.add(0f)
        }

        startAnimation()
    }

    private fun startAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 1000L
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val progress = animation.animatedFraction
            animatedFractions = segments.map { it.targetPercentage * progress }.toMutableList()
            invalidate()
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val padding = strokeWidth / 2f

        val arcDiameter = min(viewWidth, viewHeight * 1.7f) - padding * 2
        val left = (viewWidth - arcDiameter) / 2f
        val top = padding
        val right = left + arcDiameter
        val bottom = top + arcDiameter

        val oval = RectF(left, top, right, bottom)
        paint.strokeWidth = strokeWidth
        paint.color = backgroundColor
        canvas.drawArc(oval, startAngle, sweepAngleTotal, false, paint)

        var currentAngle = startAngle
        segments.forEachIndexed { index, segment ->
            val sweepPercent = animatedFractions.getOrNull(index) ?: 0f
            val sweep = sweepAngleTotal * (sweepPercent / 100f)
            paint.color = segment.color
            canvas.drawArc(oval, currentAngle, sweep, false, paint)
            currentAngle += sweep
        }

    }



}