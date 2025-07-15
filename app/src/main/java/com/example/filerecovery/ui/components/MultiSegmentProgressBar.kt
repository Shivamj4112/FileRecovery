package com.example.filerecovery.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.example.filerecovery.R


class MultiSegmentProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cornerRadius = 100f
    private val segments = mutableListOf<AnimatedSegment>()
    private var animatedFractions = mutableListOf<Float>()
    private var backgroundColor: Int = context.getColor(R.color.progress_bar_background)

    data class Segment(val percentage: Float, val color: Int)
    private data class AnimatedSegment(val targetPercentage: Float, val color: Int)

    fun setSegments(newSegments: List<Segment>, remainingColor: Int = context.getColor(R.color.progress_bar_background)) {
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
            animatedFractions = segments.map { it.targetPercentage * progress } as MutableList<Float>
            invalidate()
        }
        animator.start()
    }



    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var startX = 0f
        val height = height.toFloat()


        canvas.drawRoundRect(
            RectF(0f, 0f, width.toFloat(), height),
            cornerRadius,
            cornerRadius,
            paint.apply { color = backgroundColor }
        )


        val path = Path().apply {
            addRoundRect(
                RectF(0f, 0f, width.toFloat(), height),
                cornerRadius,
                cornerRadius,
                Path.Direction.CW
            )
        }
        canvas.clipPath(path)

        segments.forEachIndexed { index, segment ->
            val widthFraction = animatedFractions.getOrNull(index)?.div(100f) ?: 0f
            val segmentWidth = width * widthFraction
            paint.color = segment.color
            canvas.drawRect(startX, 0f, startX + segmentWidth, height, paint)
            startX += segmentWidth
        }
    }


}