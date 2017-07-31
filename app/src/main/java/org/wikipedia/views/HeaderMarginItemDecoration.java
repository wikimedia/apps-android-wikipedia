package org.wikipedia.views;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class HeaderMarginItemDecoration extends MarginItemDecoration {

    public HeaderMarginItemDecoration(@NonNull Context context, @DimenRes int topId,
                                      @DimenRes int bottomId) {
        super(0, pixelSize(context, topId), 0, pixelSize(context, bottomId));
    }

    @Override public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                         RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) == 0) {
            super.getItemOffsets(outRect, view, parent, state);
        }
    }
}
