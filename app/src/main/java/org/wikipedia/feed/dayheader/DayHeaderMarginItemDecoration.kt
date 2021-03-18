package org.wikipedia.feed.dayheader

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.views.MarginItemDecoration

class DayHeaderMarginItemDecoration(context: Context, @DimenRes bottomId: Int) :
        MarginItemDecoration(0, 0, 0, context.resources.getDimensionPixelSize(bottomId)) {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
                                state: RecyclerView.State) {
        if (view is DayHeaderCardView) {
            super.getItemOffsets(outRect, view, parent, state)
        }
    }
}
