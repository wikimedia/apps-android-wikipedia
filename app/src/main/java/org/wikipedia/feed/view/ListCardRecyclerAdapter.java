package org.wikipedia.feed.view;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;

import java.util.List;

public abstract class ListCardRecyclerAdapter<T>
        extends DefaultRecyclerAdapter<T, ListCardItemView> {
    public ListCardRecyclerAdapter(@NonNull List<T> items) {
        super(items);
    }

    @Nullable protected abstract ListCardItemView.Callback callback();

    @Override public DefaultViewHolder<ListCardItemView> onCreateViewHolder(ViewGroup parent,
                                                                            int viewType) {
        return new DefaultViewHolder<>(new ListCardItemView(parent.getContext()));
    }

    @Override public void onViewAttachedToWindow(DefaultViewHolder<ListCardItemView> holder) {
        super.onViewAttachedToWindow(holder);
        holder.getView().setCallback(callback());
    }

    @Override public void onViewDetachedFromWindow(DefaultViewHolder<ListCardItemView> holder) {
        holder.getView().setCallback(null);
        super.onViewDetachedFromWindow(holder);
    }
}
