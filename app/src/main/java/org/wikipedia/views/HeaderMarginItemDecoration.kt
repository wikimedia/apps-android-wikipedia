package org.wikipedia.views

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.util.DimenUtil.roundedDpToPx

class HeaderMarginItemDecoration : MarginItemDecoration {

    constructor(topDp: Int, bottomDp: Int) :
            super(0, roundedDpToPx(topDp.toFloat()), 0, roundedDpToPx(bottomDp.toFloat()))

    constructor(context: Context, @DimenRes topId: Int, @DimenRes bottomId: Int) :
            super(0, context.resources.getDimensionPixelSize(topId), 0, context.resources.getDimensionPixelSize(bottomId))

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
                                state: RecyclerView.State) {
        if (parent.getChildAdapterPosition(view) == 0) {
            super.getItemOffsets(outRect, view, parent, state)
        }
    }
}
