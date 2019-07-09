package org.wikipedia.views;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.wikipedia.util.DimenUtil;

public class HeaderMarginItemDecoration extends MarginItemDecoration {

    public HeaderMarginItemDecoration(int topDp, int bottomDp) {
        super(0, DimenUtil.roundedDpToPx(topDp), 0, DimenUtil.roundedDpToPx(bottomDp));
    }

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
