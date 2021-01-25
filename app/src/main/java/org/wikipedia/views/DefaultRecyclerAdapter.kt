package org.wikipedia.views

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class DefaultRecyclerAdapter<T, V : View>(val items: List<T>) : RecyclerView.Adapter<DefaultViewHolder<V>>() {
    override fun getItemCount(): Int {
        return items.size
    }

    protected fun item(position: Int): T {
        return items[position]
    }
}
