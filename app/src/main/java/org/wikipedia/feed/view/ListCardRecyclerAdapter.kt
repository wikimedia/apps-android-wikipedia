package org.wikipedia.feed.view

import android.view.ViewGroup
import org.wikipedia.views.DefaultRecyclerAdapter
import org.wikipedia.views.DefaultViewHolder

abstract class ListCardRecyclerAdapter<T>(items: List<T>) : DefaultRecyclerAdapter<T, ListCardItemView>(items) {
    protected abstract fun callback(): ListCardItemView.Callback?
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<ListCardItemView> {
        return DefaultViewHolder(ListCardItemView(parent.context))
    }

    override fun onViewAttachedToWindow(holder: DefaultViewHolder<ListCardItemView>) {
        super.onViewAttachedToWindow(holder)
        holder.view.setCallback(callback())
    }

    override fun onViewDetachedFromWindow(holder: DefaultViewHolder<ListCardItemView>) {
        holder.view.setCallback(null)
        super.onViewDetachedFromWindow(holder)
    }
}
