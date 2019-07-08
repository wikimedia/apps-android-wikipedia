package org.wikipedia.views;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

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
