package org.wikipedia.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil

class GraphView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {
    private val gradientColor1 = ResourceUtil.getThemedColor(context, R.attr.colorAccent)
    private val gradientColor2 = ContextCompat.getColor(context, R.color.green50)
    private val dataSet = mutableListOf<Float>()
    private var maxX = 0f
    private var maxY = 0f
    private val path = Path()

    private val pathPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = GRAPH_STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
        pathEffect = CornerPathEffect(GRAPH_STROKE_WIDTH)
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = GRAPH_STROKE_WIDTH
        color = Color.GRAY
    }

    fun setData(list: List<Float>) {
        maxX = list.size.toFloat()
        maxY = list.maxByOrNull { it } ?: 0f
        dataSet.clear()
        dataSet.addAll(list)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path.reset()
        dataSet.forEachIndexed { index, data ->
            if (index == 0) {
                path.moveTo(index.scaleX() + GRAPH_MARGIN, data.scaleY() + (GRAPH_MARGIN / 2))
            } else if (index < dataSet.size) {
                path.lineTo(index.scaleX() + GRAPH_MARGIN, data.scaleY() + (GRAPH_MARGIN / 2))
            }
        }
        canvas.drawPath(path, pathPaint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        pathPaint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(gradientColor1, gradientColor2),
                floatArrayOf(0f, 1f), Shader.TileMode.MIRROR)
    }

    private fun Int.scaleX() = toFloat() / maxX * (width - GRAPH_MARGIN)
    private fun Float.scaleY() = this / maxY * (height - GRAPH_MARGIN)

    companion object {
        const val GRAPH_MARGIN = 6f
        const val GRAPH_STROKE_WIDTH = 7f
    }
}
