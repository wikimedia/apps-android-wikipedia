package org.wikipedia.views

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

open class MarginItemDecoration(leftMargin: Int, topMargin: Int, rightMargin: Int, bottomMargin: Int) : ItemDecoration() {
    private val offsets = Rect()

    constructor(context: Context, @DimenRes leftId: Int, @DimenRes topId: Int, @DimenRes rightId: Int, @DimenRes bottomId: Int) :
            this(context.resources.getDimensionPixelSize(leftId), context.resources.getDimensionPixelSize(topId),
                    context.resources.getDimensionPixelSize(rightId), context.resources.getDimensionPixelSize(bottomId))

    init {
        offsets[leftMargin, topMargin, rightMargin] = bottomMargin
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView,
                                state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.set(offsets)
    }
}
