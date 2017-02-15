package org.wikipedia.views;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class MarginItemDecoration extends RecyclerView.ItemDecoration {
    private final Rect offsets = new Rect();

    public MarginItemDecoration(@NonNull Context context, @DimenRes int id) {
        this(pixelSize(context, id));
    }

    public MarginItemDecoration(@NonNull Context context, @DimenRes int leftId, @DimenRes int topId,
                                @DimenRes int rightId, @DimenRes int bottomId) {
        this(pixelSize(context, leftId), pixelSize(context, topId), pixelSize(context, rightId),
                pixelSize(context, bottomId));
    }

    public MarginItemDecoration(int margin) {
        this(margin, margin, margin, margin);
    }

    public MarginItemDecoration(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
        offsets.set(leftMargin, topMargin, rightMargin, bottomMargin);
    }

    @Override public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                         RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.set(offsets);
    }

    protected static int pixelSize(@NonNull Context context, @DimenRes int id) {
        return context.getResources().getDimensionPixelSize(id);
    }
}
