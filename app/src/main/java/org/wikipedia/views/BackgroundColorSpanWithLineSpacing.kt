package org.wikipedia.views

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.RectF
import android.text.style.ReplacementSpan
import kotlin.math.roundToInt

class BackgroundColorSpanWithLineSpacing constructor(private val backgroundColor: Int, private val textColor: Int,
                                                     private val lineSpacingAdded: Float) : ReplacementSpan() {
    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val newBottom = bottom - lineSpacingAdded
        val rect = RectF(x, top.toFloat(), x + measureText(paint, text, start, end), newBottom)
        paint.color = backgroundColor
        canvas.drawRoundRect(rect, 0f, 0f, paint)
        paint.color = textColor
        canvas.drawText(text, start, end, x, y.toFloat(), paint)
    }

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: FontMetricsInt?): Int {
        return paint.measureText(text, start, end).roundToInt()
    }

    private fun measureText(paint: Paint, text: CharSequence, start: Int, end: Int): Float {
        return paint.measureText(text, start, end)
    }
}
