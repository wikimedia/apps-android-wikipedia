package org.wikipedia.views

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.util.DimenUtil

class FooterMarginItemDecoration : MarginItemDecoration {

    constructor(topDp: Int, bottomDp: Int)
            : super(0, DimenUtil.roundedDpToPx(topDp.toFloat()), 0, DimenUtil.roundedDpToPx(bottomDp.toFloat()))

    constructor(context: Context, @DimenRes topId: Int, @DimenRes bottomId: Int)
            : super(0, pixelSize(context, topId), 0, pixelSize(context, bottomId))

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        parent.adapter
                ?.let { parent.getChildAdapterPosition(view) == it.itemCount - 1 }
                ?.let { super.getItemOffsets(outRect, view, parent, state) }
    }
}
