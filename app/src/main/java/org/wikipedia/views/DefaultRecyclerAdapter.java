package org.wikipedia.views;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

public abstract class DefaultRecyclerAdapter<T, V extends View>
        extends RecyclerView.Adapter<DefaultViewHolder<V>> {
    @NonNull private final List<T> items;

    public DefaultRecyclerAdapter(@NonNull List<T> items) {
        this.items = items;
    }

    @Override public int getItemCount() {
        return items.size();
    }

    protected T item(int position) {
        return items.get(position);
    }

    @NonNull protected List<T> items() {
        return items;
    }
}
