package org.wikipedia.views

import androidx.annotation.IntRange
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class ItemTouchHelperSwipeAdapter(private val callback: Callback) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.END) {

    interface Callback {
        fun onSwiped(@IntRange(from = 0) item: Int)
    }

    interface SwipeableView

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder): Boolean = false

    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
            if (viewHolder.itemView is SwipeableView) {
                super.getSwipeDirs(recyclerView, viewHolder)
            } else 0

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) =
            callback.onSwiped(viewHolder.adapterPosition)

}
