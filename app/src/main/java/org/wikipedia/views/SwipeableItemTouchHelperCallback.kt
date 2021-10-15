package org.wikipedia.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.wikipedia.R
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.DimenUtil.densityScalar
import org.wikipedia.util.ResourceUtil.bitmapFromVectorDrawable
import org.wikipedia.util.ResourceUtil.getThemedColor

class SwipeableItemTouchHelperCallback @JvmOverloads constructor(
        private val context: Context,
        @ColorRes swipeColor: Int = R.color.red50,
        @DrawableRes swipeIcon: Int = R.drawable.ic_delete_white_24dp,
        @ColorRes val swipeIconTint: Int? = null,
        private val swipeIconAndTextFromTag: Boolean = false,
        val refreshLayout: SwipeRefreshLayout? = null
) : ItemTouchHelper.Callback() {

    interface Callback {
        fun onSwipe()
    }

    private lateinit var swipeIconBitmap: Bitmap
    private val swipeBackgroundPaint = Paint()
    private val swipeIconPaint = Paint()
    private val itemBackgroundPaint = Paint()
    private val valueTextPaint = Paint().apply {
        color = ContextCompat.getColor(context, swipeIconTint ?: android.R.color.white)
        textSize = DimenUtil.dpToPx(12f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
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

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        refreshLayout?.isEnabled = actionState != ItemTouchHelper.ACTION_STATE_SWIPE
    }

    override fun onChildDraw(canvas: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                             dx: Float, dy: Float, actionState: Int, isCurrentlyActive: Boolean) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // Text and icon sources from itemView tag.
            var swipeText = ""
            if (swipeIconAndTextFromTag &&
                viewHolder.itemView.getTag(R.string.tag_icon_key) != null &&
                viewHolder.itemView.getTag(R.string.tag_text_key) != null) {
                swipeText = viewHolder.itemView.getTag(R.string.tag_text_key).toString()
                swipeIconBitmap = bitmapFromVectorDrawable(context, viewHolder.itemView.getTag(R.string.tag_icon_key) as Int, swipeIconTint)
            }

            canvas.drawRect(0f, viewHolder.itemView.top.toFloat(), viewHolder.itemView.width.toFloat(), (viewHolder.itemView.top + viewHolder.itemView.height).toFloat(), swipeBackgroundPaint)
            val iconPositionY = (viewHolder.itemView.top + (viewHolder.itemView.height / 2 - (if (swipeIconAndTextFromTag) (swipeIconBitmap.height / SWIPE_ICON_POSITION_SCALE).toInt() else swipeIconBitmap.height / 2))).toFloat()
            val iconTextPositionY = viewHolder.itemView.top + (viewHolder.itemView.height / 2 + swipeIconBitmap.height) - SWIPE_ICON_PADDING_DP + SWIPE_TEXT_PADDING_DP
            if (dx >= 0) {
                canvas.drawBitmap(swipeIconBitmap, SWIPE_ICON_PADDING_DP * SWIPE_ICON_POSITION_SCALE * densityScalar, iconPositionY, swipeIconPaint)
                if (swipeIconAndTextFromTag) {
                    canvas.drawText(swipeText, swipeIconBitmap.width + SWIPE_ICON_PADDING_DP * SWIPE_TEXT_POSITION_SCALE,
                        iconTextPositionY, valueTextPaint)
                }
            } else {
                canvas.drawBitmap(swipeIconBitmap, viewHolder.itemView.right - swipeIconBitmap.width - SWIPE_ICON_PADDING_DP * SWIPE_ICON_POSITION_SCALE * densityScalar, iconPositionY, swipeIconPaint)
                if (swipeIconAndTextFromTag) {
                    canvas.drawText(swipeText, viewHolder.itemView.right - swipeIconBitmap.width - SWIPE_ICON_PADDING_DP * SWIPE_TEXT_POSITION_SCALE,
                        iconTextPositionY, valueTextPaint)
                }
            }
            canvas.drawRect(dx, viewHolder.itemView.top.toFloat(), viewHolder.itemView.width + dx, (viewHolder.itemView.top + viewHolder.itemView.height).toFloat(), itemBackgroundPaint)
            viewHolder.itemView.translationX = dx
        } else {
            super.onChildDraw(canvas, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive)
        }
    }

    companion object {
        private const val SWIPE_ICON_PADDING_DP = 16f
        private const val SWIPE_TEXT_PADDING_DP = 2f
        private const val SWIPE_ICON_POSITION_SCALE = 1.4f
        private const val SWIPE_TEXT_POSITION_SCALE = 2f
    }
}
