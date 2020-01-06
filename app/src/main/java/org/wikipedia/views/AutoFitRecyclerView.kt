package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.IntRange
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.util.log.L
import kotlin.math.max

/** [RecyclerView] that invokes a callback when the number of columns should be updated.  */
open class AutoFitRecyclerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {

    interface Callback {
        fun onColumns(columns: Int)
    }

    @IntRange(from = MIN_COLUMNS.toLong())
    var columns = MIN_COLUMNS
        private set
    private var minColumnWidth = 0
    private var callback: Callback = DefaultCallback()

    init {
        if (attrs != null) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.AutoFitRecyclerView, defStyleAttr, 0)
            minColumnWidth = array.getDimensionPixelSize(R.styleable.AutoFitRecyclerView_minColumnWidth, 0)
            array.recycle()
        }
    }

    fun minColumnWidth(minColumnWidth: Int) {
        if (this.minColumnWidth != minColumnWidth) {
            this.minColumnWidth = minColumnWidth
            requestLayout()
        }
    }

    fun setCallback(callback: Callback?) {
        this.callback = callback ?: DefaultCallback()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        val cols = calculateColumns(minColumnWidth, measuredWidth)
        if (columns != cols) {
            columns = cols
            callback.onColumns(columns)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // https://issuetracker.google.com/issues/37034096
        // TODO: check again in Sep 2018
        try {
            super.onLayout(changed, l, t, r, b)
        } catch (e: Exception) {
            L.logRemoteErrorIfProd(e)
        }
    }

    private fun calculateColumns(columnWidth: Int, availableWidth: Int): Int =
            if (columnWidth > 0) max(MIN_COLUMNS, availableWidth / columnWidth) else MIN_COLUMNS

    class DefaultCallback : Callback {
        override fun onColumns(columns: Int) = Unit
    }

    companion object {
        private const val MIN_COLUMNS = 1
    }
}
