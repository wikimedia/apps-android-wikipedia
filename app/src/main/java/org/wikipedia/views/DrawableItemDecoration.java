package org.wikipedia.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class DrawableItemDecoration extends RecyclerView.ItemDecoration {
    @Nullable private final Drawable drawable;
    private final boolean drawEnds;

    public DrawableItemDecoration(@NonNull Context context, @DrawableRes int id,
                                  boolean drawEnds) {
        this(ContextCompat.getDrawable(context, id), drawEnds);
    }

    public DrawableItemDecoration(@Nullable Drawable drawable, boolean drawEnds) {
        this.drawable = drawable;
        this.drawEnds = drawEnds;
    }

    @Override public void onDraw(Canvas canvas, @NonNull RecyclerView parent,
                                 RecyclerView.State state) {
        super.onDraw(canvas, parent, state);

        if (drawable == null || parent.getChildCount() == 0) {
            return;
        }

        int end = parent.getChildCount() - 1;
        onDrawHeader(canvas, parent, state, parent.getChildAt(0));
        for (int i = 1; i < end; ++i) {
            View child = parent.getChildAt(i);
            onDrawItem(canvas, parent, state, child);
        }
        onDrawFooter(canvas, parent, state, parent.getChildAt(end));
    }

    private void onDrawHeader(Canvas canvas, @NonNull RecyclerView parent,
                              RecyclerView.State state, @NonNull View child) {
        if (drawEnds) {
            draw(canvas, bounds(parent, child, true));
        }
        draw(canvas, bounds(parent, child, false));
    }

    private void onDrawFooter(Canvas canvas, @NonNull RecyclerView parent,
                              RecyclerView.State state, @NonNull View child) {
        if (drawEnds) {
            draw(canvas, bounds(parent, child, false));
        }
    }

    private void onDrawItem(Canvas canvas, @NonNull RecyclerView parent, RecyclerView.State state,
                            @NonNull View child) {

        draw(canvas, bounds(parent, child, false));
    }

    private Rect bounds(@NonNull RecyclerView parent, @NonNull View child, boolean top) {
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();

        Rect bounds = new Rect();
        bounds.right = parent.getWidth() - parent.getPaddingRight();
        bounds.left = parent.getPaddingLeft();
        int height = drawable.getIntrinsicHeight();
        bounds.top = top
                ? layoutManager.getDecoratedTop(child)
                : layoutManager.getDecoratedBottom(child) - height;
        bounds.bottom = bounds.top + height;

        return bounds;
    }

    private void draw(Canvas canvas, @NonNull Rect bounds) {
        drawable.setBounds(bounds);
        drawable.draw(canvas);
    }
}
