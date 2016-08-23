package org.wikipedia.feed.view;

import android.support.annotation.NonNull;
import android.view.ViewGroup;

import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;

import java.util.List;

public abstract class PageTitleRecyclerAdapter<T>
        extends DefaultRecyclerAdapter<T, PageTitleListCardItemView> {
    public PageTitleRecyclerAdapter(@NonNull List<T> items) {
        super(items);
    }

    @Override public DefaultViewHolder<PageTitleListCardItemView> onCreateViewHolder(ViewGroup parent,
                                                                                     int viewType) {
        return new DefaultViewHolder<>(new PageTitleListCardItemView(parent.getContext()));
    }
}