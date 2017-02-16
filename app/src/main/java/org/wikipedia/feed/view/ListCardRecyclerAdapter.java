package org.wikipedia.feed.view;

import android.support.annotation.NonNull;
import android.view.ViewGroup;

import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;

import java.util.List;

public abstract class ListCardRecyclerAdapter<T>
        extends DefaultRecyclerAdapter<T, ListCardItemView> {
    public ListCardRecyclerAdapter(@NonNull List<T> items) {
        super(items);
    }

    @Override public DefaultViewHolder<ListCardItemView> onCreateViewHolder(ViewGroup parent,
                                                                            int viewType) {
        return new DefaultViewHolder<>(new ListCardItemView(parent.getContext()));
    }
}
