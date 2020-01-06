package org.wikipedia.views

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

open class MarginItemDecoration(leftMargin: Int, topMargin: Int, rightMargin: Int, bottomMargin: Int) : ItemDecoration() {

    private val offsets = Rect(leftMargin, topMargin, rightMargin, bottomMargin)

    constructor(context: Context, @DimenRes id: Int) : this(pixelSize(context, id))

    constructor(context: Context, @DimenRes leftId: Int, @DimenRes topId: Int, @DimenRes rightId: Int, @DimenRes bottomId: Int)
            : this(pixelSize(context, leftId), pixelSize(context, topId), pixelSize(context, rightId), pixelSize(context, bottomId))

    constructor(margin: Int) : this(margin, margin, margin, margin)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.set(offsets)
    }

    companion object {
        @JvmStatic
        protected fun pixelSize(context: Context, @DimenRes id: Int): Int = context.resources.getDimensionPixelSize(id)
    }
}
