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
    private val path = Path()

    private val pathPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = GRAPH_STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
        pathEffect = CornerPathEffect(GRAPH_STROKE_WIDTH)
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
        path.reset()
        dataSet.forEachIndexed { index, data ->
            if (index == 0) {
                path.moveTo(index.scaleX() + GRAPH_MARGIN, data.views.scaleY() + (GRAPH_MARGIN / 2))
            } else if (index < dataSet.size) {
                path.lineTo(index.scaleX() + GRAPH_MARGIN, data.views.scaleY() + (GRAPH_MARGIN / 2))
            }
        }
        canvas.drawPath(path, pathPaint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        pathPaint.shader =  LinearGradient(0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(gradientColor2, gradientColor1, gradientColor2),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.MIRROR)
    }

    private fun Int.scaleX() = toFloat() / maxX * (width - GRAPH_MARGIN)
    private fun Int.scaleY() = toFloat() / maxY * (height - GRAPH_MARGIN)

    companion object {
        const val GRAPH_MARGIN = 6f
        const val GRAPH_STROKE_WIDTH = 7f
    }
}
