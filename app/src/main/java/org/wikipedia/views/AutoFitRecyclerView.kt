package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import org.wikipedia.R
import org.wikipedia.util.log.L.logRemoteErrorIfProd

/** [RecyclerView] that invokes a callback when the number of columns should be updated.  */
open class AutoFitRecyclerView(context: Context, attrs: AttributeSet? = null) :
        RecyclerView(context, attrs) {

    interface Callback {
        fun onColumns(columns: Int)
    }

    protected var recyclerLayoutManager: StaggeredGridLayoutManager
    private var minColumnWidth = 0
    private var minColumnCount = 1
    var columns = 0
    var callback: Callback = DefaultCallback()

    init {
        context.obtainStyledAttributes(attrs, R.styleable.AutoFitRecyclerView).use {
            minColumnWidth = it.getDimensionPixelSize(R.styleable.AutoFitRecyclerView_minColumnWidth, 0)
            minColumnCount = it.getInt(R.styleable.AutoFitRecyclerView_minColumnCount, 1)
        }
        columns = minColumnCount
        recyclerLayoutManager = StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)
        layoutManager = recyclerLayoutManager
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        val cols = calculateColumns(minColumnWidth, measuredWidth)
        if (columns != cols) {
            columns = cols
            recyclerLayoutManager.spanCount = columns
            callback.onColumns(columns)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // https://issuetracker.google.com/issues/37034096
        // TODO: check again in August 2024
        try {
            super.onLayout(changed, l, t, r, b)
        } catch (e: Exception) {
            logRemoteErrorIfProd(e)
        }
    }

    private fun calculateColumns(columnWidth: Int, availableWidth: Int): Int {
        return if (columnWidth > 0) minColumnCount.coerceAtLeast(availableWidth / columnWidth) else minColumnCount
    }

    private class DefaultCallback : Callback {
        override fun onColumns(columns: Int) {}
    }
}
