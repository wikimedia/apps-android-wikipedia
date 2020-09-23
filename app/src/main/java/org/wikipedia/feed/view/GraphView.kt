package org.wikipedia.feed.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.feed.mostread.MostReadArticles


class GraphView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {
    private val gradientColor1 = ContextCompat.getColor(context, R.color.accent50)
    private val gradientColor2 = ContextCompat.getColor(context, R.color.green50)
    private val dataSet = mutableListOf<MostReadArticles.ViewHistory>()
    private var maxX = 0
    private var maxY = 0

    private val linePaint = Paint().apply {
//        val paintShader: Shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(),
//                intArrayOf(gradientColor2, gradientColor1, gradientColor2),
//                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.REPEAT)
//        shader = paintShader
        color = Color.BLACK
        strokeWidth = 7f
        isAntiAlias = true
    }

    fun setData(newDataSet: List<MostReadArticles.ViewHistory>) {
        maxX = newDataSet.size
        maxY = newDataSet.maxByOrNull { it.views }?.views ?: 0
        dataSet.clear()
        dataSet.addAll(newDataSet)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        dataSet.forEachIndexed { index, currentDataPoint ->
            if (index < dataSet.size - 1) {
                val nextDataPoint = dataSet[index + 1]
                val startX = index.scaleX()
                val startY = currentDataPoint.views.scaleY()
                val endX = (index + 1).scaleX()
                val endY = nextDataPoint.views.scaleY()
                canvas.drawLine(startX, startY, endX, endY, linePaint)
            }
        }
    }

    private fun Int.scaleX() = toFloat() / maxX * width
    private fun Int.scaleY() = toFloat() / maxY * height
}
