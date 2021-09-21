package org.wikipedia.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.DimenUtil.densityScalar
import org.wikipedia.util.ResourceUtil.bitmapFromVectorDrawable
import org.wikipedia.util.ResourceUtil.getThemedColor

class SwipeableItemTouchHelperCallback @JvmOverloads constructor(
        context: Context,
        @ColorRes swipeColor: Int = R.color.red50,
        @DrawableRes swipeIcon: Int = R.drawable.ic_delete_white_24dp,
        @ColorRes swipeIconTint: Int? = null
) : ItemTouchHelper.Callback() {

    interface Callback {
        fun onSwipe()
    }

    private val swipeBackgroundPaint = Paint()
    private val swipeIconPaint = Paint()
    private val itemBackgroundPaint = Paint()
    private val swipeIconBitmap: Bitmap
    private val valueTextPaint = Paint().apply {
        color = ContextCompat.getColor(context, swipeIconTint ?: android.R.color.white)
        textSize = DimenUtil.dpToPx(16f)
        textAlign = Paint.Align.CENTER
    }
    var swipeableEnabled = false

    init {
        swipeBackgroundPaint.style = Paint.Style.FILL
        swipeBackgroundPaint.color = ContextCompat.getColor(context, swipeColor)
        itemBackgroundPaint.style = Paint.Style.FILL
        itemBackgroundPaint.color = getThemedColor(context, android.R.attr.windowBackground)
        swipeIconBitmap = bitmapFromVectorDrawable(context, swipeIcon, swipeIconTint)
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return swipeableEnabled
    }

    override fun getMovementFlags(recyclerView: RecyclerView, holder: RecyclerView.ViewHolder): Int {
        val dragFlags = 0 // ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        val swipeFlags = if (holder is Callback) ItemTouchHelper.START or ItemTouchHelper.END else 0
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
            if (dx >= 0) {
                canvas.drawBitmap(swipeIconBitmap, SWIPE_ICON_PADDING_DP * densityScalar, (viewHolder.itemView.top + (viewHolder.itemView.height / 2 - swipeIconBitmap.height / 2)).toFloat(), swipeIconPaint)
                canvas.drawText("Unread", swipeIconBitmap.width.toFloat() + SWIPE_ICON_PADDING_DP, viewHolder.itemView.top + (viewHolder.itemView.height / 2 + swipeIconBitmap.height) + SWIPE_ICON_PADDING_DP, valueTextPaint)
            } else {
                canvas.drawBitmap(swipeIconBitmap, viewHolder.itemView.right - swipeIconBitmap.width - SWIPE_ICON_PADDING_DP * densityScalar, (viewHolder.itemView.top + (viewHolder.itemView.height / 2 - swipeIconBitmap.height / 2)).toFloat(), swipeIconPaint)
                canvas.drawText("Unread", viewHolder.itemView.right - swipeIconBitmap.width - SWIPE_ICON_PADDING_DP, viewHolder.itemView.top + (viewHolder.itemView.height / 2 + swipeIconBitmap.height) + SWIPE_ICON_PADDING_DP, valueTextPaint)
            }
            canvas.drawRect(dx, viewHolder.itemView.top.toFloat(), viewHolder.itemView.width + dx, (viewHolder.itemView.top + viewHolder.itemView.height).toFloat(), itemBackgroundPaint)
            viewHolder.itemView.translationX = dx
        } else {
            super.onChildDraw(canvas, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive)
        }
    }

    companion object {
        private const val SWIPE_ICON_PADDING_DP = 16f
    }
}
