package org.wikipedia.page.tabs

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil.getThemedColor
import kotlin.math.abs

class SwipeableTabTouchHelperCallback(context: Context) : ItemTouchHelper.Callback() {

    interface Callback {
        fun onSwipe()
        fun isSwipeable(): Boolean
    }

    private val swipeBackgroundPaint = Paint()
    private val itemBackgroundPaint = Paint()
    var swipeableEnabled = false

    init {
        swipeBackgroundPaint.style = Paint.Style.FILL
        swipeBackgroundPaint.color = getThemedColor(context, R.attr.background_color)
        itemBackgroundPaint.style = Paint.Style.FILL
        itemBackgroundPaint.color = swipeBackgroundPaint.color
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return swipeableEnabled
    }

    override fun getMovementFlags(recyclerView: RecyclerView, holder: RecyclerView.ViewHolder): Int {
        val dragFlags = 0 // ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        val swipeFlags = if (holder is Callback && holder.isSwipeable()) ItemTouchHelper.START or ItemTouchHelper.END else 0
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(recyclerView: RecyclerView, source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return source.itemViewType == target.itemViewType
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
        if (viewHolder is Callback) {
            viewHolder.onSwipe()
        }
    }

    override fun onChildDraw(canvas: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                             dx: Float, dy: Float, actionState: Int, isCurrentlyActive: Boolean) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            canvas.drawRect(0f, viewHolder.itemView.top.toFloat(), viewHolder.itemView.width.toFloat(), (viewHolder.itemView.top + viewHolder.itemView.height).toFloat(), swipeBackgroundPaint)
            canvas.drawRect(dx, viewHolder.itemView.top.toFloat(), viewHolder.itemView.width + dx, (viewHolder.itemView.top + viewHolder.itemView.height).toFloat(), itemBackgroundPaint)
            viewHolder.itemView.translationX = dx
            viewHolder.itemView.alpha = 1 - abs(dx) / viewHolder.itemView.width
        } else {
            super.onChildDraw(canvas, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive)
        }
    }
}
