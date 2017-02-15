package org.wikipedia.views;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/** The minimum boilerplate required by the view holder pattern for use with custom views. */
public class DefaultViewHolder<T extends View> extends RecyclerView.ViewHolder {
    public DefaultViewHolder(@NonNull T view) {
        super(view);
    }

    @NonNull public T getView() {
        //noinspection unchecked
        return (T) itemView;
    }
}
