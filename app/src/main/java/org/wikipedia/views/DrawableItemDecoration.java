package org.wikipedia.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class DrawableItemDecoration extends RecyclerView.ItemDecoration {
    @Nullable private final Drawable drawable;

    public DrawableItemDecoration(@NonNull Context context, @DrawableRes int id) {
        this(ContextCompat.getDrawable(context, id));
    }

    public DrawableItemDecoration(@Nullable Drawable drawable) {
        this.drawable = drawable;
    }

    @Override public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(canvas, parent, state);

        if (drawable == null) {
            return;
        }

        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();

        for (int i = 0; i < parent.getChildCount(); ++i) {
            View child = parent.getChildAt(i);
            int top = layoutManager.getDecoratedBottom(child);
            int bottom = top + drawable.getIntrinsicHeight();
            drawable.setBounds(left, top, right, bottom);
            drawable.draw(canvas);
        }
    }
}