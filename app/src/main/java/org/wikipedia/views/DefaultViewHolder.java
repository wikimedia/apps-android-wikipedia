package org.wikipedia.views;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
