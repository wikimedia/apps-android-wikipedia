package org.wikipedia.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;

public class SwipeableItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private static final float DELETE_ICON_PADDING_DP = 16f;
    private Paint deleteBackgroundPaint = new Paint();
    private Paint deleteIconPaint = new Paint();
    private Paint itemBackgroundPaint = new Paint();
    @NonNull private Bitmap deleteIcon;

    public interface Callback {
        void onSwipe();
    }

    public SwipeableItemTouchHelperCallback(@NonNull Context context) {
        deleteBackgroundPaint.setStyle(Paint.Style.FILL);
        deleteBackgroundPaint.setColor(ContextCompat.getColor(context, R.color.red50));
        itemBackgroundPaint.setStyle(Paint.Style.FILL);
        itemBackgroundPaint.setColor(ResourceUtil.getThemedColor(context, android.R.attr.windowBackground));
        deleteIcon = ResourceUtil.bitmapFromVectorDrawable(context, R.drawable.ic_delete_white_24dp);
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder holder) {
        final int dragFlags = 0; //ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        final int swipeFlags = holder instanceof Callback
                ? ItemTouchHelper.START | ItemTouchHelper.END : 0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        return source.getItemViewType() == target.getItemViewType();
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
        if (viewHolder instanceof Callback) {
            ((Callback) viewHolder).onSwipe();
        }
    }

    @Override
    public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dx, float dy, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            canvas.drawRect(0f, viewHolder.itemView.getTop(), viewHolder.itemView.getWidth(), viewHolder.itemView.getTop() + viewHolder.itemView.getHeight(), deleteBackgroundPaint);
            canvas.drawRect(dx, viewHolder.itemView.getTop(), viewHolder.itemView.getWidth() + dx, viewHolder.itemView.getTop() + viewHolder.itemView.getHeight(), itemBackgroundPaint);
            if (dx >= 0) {
                canvas.drawBitmap(deleteIcon, DELETE_ICON_PADDING_DP * DimenUtil.getDensityScalar(), viewHolder.itemView.getTop() + (viewHolder.itemView.getHeight() / 2 - deleteIcon.getHeight() / 2), deleteIconPaint);
            } else {
                canvas.drawBitmap(deleteIcon, viewHolder.itemView.getRight() - deleteIcon.getWidth() - DELETE_ICON_PADDING_DP * DimenUtil.getDensityScalar(), viewHolder.itemView.getTop() + (viewHolder.itemView.getHeight() / 2 - deleteIcon.getHeight() / 2), deleteIconPaint);
            }
            viewHolder.itemView.setTranslationX(dx);
        } else {
            super.onChildDraw(canvas, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive);
        }
    }
}
