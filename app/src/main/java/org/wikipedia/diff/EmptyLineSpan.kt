package org.wikipedia.diff

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan
import org.wikipedia.R
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

class EmptyLineSpan(context: Context) : LineBackgroundSpan {
    private val fillColor = ResourceUtil.getThemedColor(context, android.R.attr.colorBackground)
    private val strokeColor = ResourceUtil.getThemedColor(context, R.attr.placeholder_color)

    override fun drawBackground(canvas: Canvas, paint: Paint, left: Int, right: Int, top: Int, baseline: Int, bottom: Int, text: CharSequence, start: Int, end: Int, lineNumber: Int) {
        val prevColor = paint.color
        val prevStyle = paint.style
        val strokeAdjust = DimenUtil.dpToPx(0.5f)
        val roundRadius = DimenUtil.dpToPx(8f)

        paint.style = Paint.Style.FILL
        paint.color = fillColor
        canvas.drawRoundRect(left.toFloat() + strokeAdjust, top.toFloat() + strokeAdjust,
                right.toFloat() - strokeAdjust, bottom.toFloat() + roundRadius / 2f, roundRadius, roundRadius, paint)
        paint.style = Paint.Style.STROKE
        paint.color = strokeColor
        canvas.drawRoundRect(left.toFloat() + strokeAdjust, top.toFloat() + strokeAdjust,
                right.toFloat() - strokeAdjust, bottom.toFloat() + roundRadius / 2f, roundRadius, roundRadius, paint)

        paint.style = prevStyle
        paint.color = prevColor
    }
}
