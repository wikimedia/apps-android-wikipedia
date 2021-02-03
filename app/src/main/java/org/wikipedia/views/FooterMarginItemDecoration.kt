package org.wikipedia.views

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.util.DimenUtil.roundedDpToPx

class FooterMarginItemDecoration(topDp: Int, bottomDp: Int) :
        MarginItemDecoration(0, roundedDpToPx(topDp.toFloat()), 0, roundedDpToPx(bottomDp.toFloat())) {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.adapter != null && parent.getChildAdapterPosition(view) == parent.adapter!!.itemCount - 1) {
            super.getItemOffsets(outRect, view, parent, state)
        }
    }
}
