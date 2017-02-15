package org.wikipedia.views;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

public class ItemTouchHelperSwipeAdapter extends ItemTouchHelper.SimpleCallback {
    public interface Callback {
        void onSwiped(@IntRange(from = 0) int item);
    }

    public interface SwipeableView { }

    @NonNull private final Callback callback;

    public ItemTouchHelperSwipeAdapter(@NonNull Callback callback) {
        super(0, ItemTouchHelper.END);
        this.callback = callback;
    }

    @Override public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                    RecyclerView.ViewHolder target) {
        return false;
    }

    @Override public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if (viewHolder.itemView instanceof SwipeableView) {
            return super.getSwipeDirs(recyclerView, viewHolder);
        }
        return 0;
    }

    @Override public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        callback.onSwiped(viewHolder.getAdapterPosition());
    }
}
