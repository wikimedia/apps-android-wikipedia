package org.wikipedia.feed.dayheader;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.wikipedia.views.MarginItemDecoration;

public class DayHeaderMarginItemDecoration extends MarginItemDecoration {

    public DayHeaderMarginItemDecoration(@NonNull Context context, @DimenRes int bottomId) {
        super(0, 0, 0, pixelSize(context, bottomId));
    }

    @Override public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                         RecyclerView.State state) {
        if (view instanceof DayHeaderCardView) {
            super.getItemOffsets(outRect, view, parent, state);
        }
    }
}
