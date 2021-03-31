package org.wikipedia.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.withStyledAttributes
import org.wikipedia.R
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

class CircularProgressBar constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var progressPaint: Paint = Paint()
    private var progressBackgroundPaint: Paint = Paint()
    private var sweepAngle = 0
    private var circleBounds: RectF = RectF()
    private var maxProgressValue = MAX_PROGRESS.toDouble()

    init {
        var progressColor = ResourceUtil.getThemedColor(getContext(), R.attr.colorAccent)
        var progressBackgroundColor = ResourceUtil.getThemedColor(getContext(), R.attr.material_theme_de_emphasised_color)
        var progressStrokeWidth = DimenUtil.dpToPx(DEFAULT_STROKE_WIDTH_DP.toFloat()).toInt()
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.CircularProgressBar) {
                progressColor = getColor(R.styleable.CircularProgressBar_progressColor, progressColor)
                progressBackgroundColor = getColor(R.styleable.CircularProgressBar_progressBackgroundColor, progressBackgroundColor)
                progressStrokeWidth = getDimensionPixelSize(R.styleable.CircularProgressBar_progressStrokeWidth, progressStrokeWidth)
                maxProgressValue = getInt(R.styleable.CircularProgressBar_maxProgress, MAX_PROGRESS).toDouble()
            }
        }
        progressPaint.strokeWidth = progressStrokeWidth.toFloat()
        progressPaint.style = Paint.Style.FILL
        progressPaint.color = progressColor
        progressPaint.isAntiAlias = true
        progressBackgroundPaint.strokeWidth = progressStrokeWidth.toFloat()
        progressBackgroundPaint.style = Paint.Style.FILL
        progressBackgroundPaint.color = progressBackgroundColor
        progressBackgroundPaint.isAntiAlias = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val paddingLeft = paddingLeft
        val paddingRight = paddingRight
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val strokeSizeOffset = progressPaint.strokeWidth
        var desiredSize = strokeSizeOffset.toInt() + (paddingBottom + paddingTop).coerceAtLeast(paddingLeft + paddingRight)
        desiredSize += desiredSize * .1f.toInt()
        val finalWidth = when (widthMode) {
            MeasureSpec.EXACTLY -> measuredWidth
            MeasureSpec.AT_MOST -> desiredSize.coerceAtMost(measuredWidth)
            MeasureSpec.UNSPECIFIED -> desiredSize
            else -> desiredSize
        }
        val finalHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> measuredHeight
            MeasureSpec.AT_MOST -> desiredSize.coerceAtMost(measuredHeight)
            MeasureSpec.UNSPECIFIED -> desiredSize
            else -> desiredSize
        }
        val widthWithoutPadding = finalWidth - paddingLeft - paddingRight
        val heightWithoutPadding = finalHeight - paddingTop - paddingBottom
        val smallestSide = heightWithoutPadding.coerceAtMost(widthWithoutPadding)
        setMeasuredDimension(smallestSide, smallestSide)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        calculateBounds(w, h)
    }

    private fun calculateBounds(w: Int, h: Int) {
        val strokeSizeOffset = progressPaint.strokeWidth
        val halfOffset = strokeSizeOffset / 2f
        circleBounds.left = halfOffset
        circleBounds.top = halfOffset
        circleBounds.right = w - halfOffset
        circleBounds.bottom = h - halfOffset
    }

    override fun onDraw(canvas: Canvas) {
        drawProgressBackground(canvas)
        drawProgress(canvas)
    }

    private fun drawProgressBackground(canvas: Canvas) {
        canvas.drawArc(circleBounds, PROGRESS_BACKGROUND_MIN_ANGLE.toFloat(), PROGRESS_BACKGROUND_MAX_ANGLE.toFloat(),
                false, progressBackgroundPaint)
    }

    private fun drawProgress(canvas: Canvas) {
        canvas.drawArc(circleBounds, PROGRESS_START_ANGLE.toFloat(), sweepAngle.toFloat(), true, progressPaint)
    }

    fun setCurrentProgress(currentProgress: Double) {
        if (currentProgress > maxProgressValue) {
            maxProgressValue = currentProgress
        }
        sweepAngle = (currentProgress / maxProgressValue * PROGRESS_BACKGROUND_MAX_ANGLE).toInt()
        invalidate()
    }

    @get:ColorInt
    var progressColor: Int
        get() = progressPaint.color
        set(color) {
            progressPaint.color = color
            invalidate()
        }

    @get:ColorInt
    var progressBackgroundColor: Int
        get() = progressBackgroundPaint.color
        set(color) {
            progressBackgroundPaint.color = color
            invalidate()
        }

    companion object {
        private const val PROGRESS_START_ANGLE = 270
        private const val PROGRESS_BACKGROUND_MIN_ANGLE = 0
        private const val PROGRESS_BACKGROUND_MAX_ANGLE = 360
        private const val DEFAULT_STROKE_WIDTH_DP = 0
        const val MIN_PROGRESS = 5
        const val MAX_PROGRESS = 100
    }
}
