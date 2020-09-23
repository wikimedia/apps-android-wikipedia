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
        color = Color.BLACK
        strokeWidth = 7f
        isAntiAlias = true
    }

    fun setData(viewHistoryList: List<MostReadArticles.ViewHistory>) {
        maxX = viewHistoryList.size
        maxY = viewHistoryList.maxByOrNull { it.views }?.views ?: 0
        dataSet.clear()
        dataSet.addAll(viewHistoryList)
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
