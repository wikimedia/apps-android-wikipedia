package org.wikipedia.feed.news

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

class RecyclerViewIndicatorDotDecor(private val radius: Float,
                                    private val indicatorItemPadding: Int,
                                    private val indicatorHeight: Int,
                                    @ColorInt colorInactive: Int,
                                    @ColorInt colorActive: Int,
                                    private val rtl: Boolean) : ItemDecoration() {

    private val inactivePaint = Paint()
    private val activePaint = Paint()

    init {
        val strokeWidth = Resources.getSystem().displayMetrics.density
        inactivePaint.strokeCap = Paint.Cap.ROUND
        inactivePaint.strokeWidth = strokeWidth
        inactivePaint.style = Paint.Style.FILL
        inactivePaint.isAntiAlias = true
        inactivePaint.color = colorInactive
        activePaint.strokeCap = Paint.Cap.ROUND
        activePaint.strokeWidth = strokeWidth
        activePaint.style = Paint.Style.FILL
        activePaint.isAntiAlias = true
        activePaint.color = colorActive
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(canvas, parent, state)
        val adapter = parent.adapter ?: return
        val itemCount = adapter.itemCount

        // horizontally centered
        val totalLength = radius * 2 * itemCount
        val paddingBetweenItems = (0.coerceAtLeast(itemCount - 1) * indicatorItemPadding)
        val indicatorTotalWidth = totalLength + paddingBetweenItems
        val indicatorStartX = (parent.width - indicatorTotalWidth) / 2f

        // vertically centered
        val indicatorPositionY = parent.height - indicatorHeight / 2f
        drawInactiveDots(canvas, indicatorStartX, indicatorPositionY, itemCount)
        var activePosition = when (parent.layoutManager) {
            is GridLayoutManager -> (parent.layoutManager as GridLayoutManager).findFirstVisibleItemPosition()
            is LinearLayoutManager -> (parent.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            else -> return
        }
        if (activePosition == RecyclerView.NO_POSITION) {
            return
        }
        if (rtl) {
            activePosition = itemCount - activePosition - 1
        }
        drawActiveDot(canvas, indicatorStartX, indicatorPositionY, activePosition)
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.bottom = indicatorHeight
    }

    private fun drawInactiveDots(canvas: Canvas, indicatorStartX: Float, indicatorPositionY: Float, itemCount: Int) {
        val itemWidth = radius * 2 + indicatorItemPadding
        var start = indicatorStartX + radius
        for (i in 0 until itemCount) {
            canvas.drawCircle(start, indicatorPositionY, radius, inactivePaint)
            start += itemWidth
        }
    }

    private fun drawActiveDot(canvas: Canvas, indicatorStartX: Float, indicatorPositionY: Float, highlightPosition: Int) {
        val itemWidth = radius * 2 + indicatorItemPadding
        val highlightStart = indicatorStartX + radius + itemWidth * highlightPosition
        canvas.drawCircle(highlightStart, indicatorPositionY, radius, activePaint)
    }
}
