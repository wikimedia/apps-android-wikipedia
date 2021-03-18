package org.wikipedia.feed.news;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewIndicatorDotDecor extends RecyclerView.ItemDecoration {

    private final int indicatorHeight;
    private final int indicatorItemPadding;
    private final int radius;
    private final boolean rtl;

    private final Paint inactivePaint = new Paint();
    private final Paint activePaint = new Paint();

    public RecyclerViewIndicatorDotDecor(int radius, int padding, int indicatorHeight, @ColorInt int colorInactive,
                                         @ColorInt int colorActive, boolean rtl) {
        this.radius = radius;
        this.indicatorItemPadding = padding;
        this.indicatorHeight = indicatorHeight;
        this.rtl = rtl;

        float strokeWidth = Resources.getSystem().getDisplayMetrics().density * 1;

        inactivePaint.setStrokeCap(Paint.Cap.ROUND);
        inactivePaint.setStrokeWidth(strokeWidth);
        inactivePaint.setStyle(Paint.Style.FILL);
        inactivePaint.setAntiAlias(true);
        inactivePaint.setColor(colorInactive);

        activePaint.setStrokeCap(Paint.Cap.ROUND);
        activePaint.setStrokeWidth(strokeWidth);
        activePaint.setStyle(Paint.Style.FILL);
        activePaint.setAntiAlias(true);
        activePaint.setColor(colorActive);
    }

    @Override
    public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(canvas, parent, state);

        final RecyclerView.Adapter adapter = parent.getAdapter();

        if (adapter == null) {
            return;
        }

        int itemCount = adapter.getItemCount();

        // horizontally centered
        float totalLength = this.radius * 2 * itemCount;
        float paddingBetweenItems = Math.max(0, itemCount - 1) * indicatorItemPadding;
        float indicatorTotalWidth = totalLength + paddingBetweenItems;
        float indicatorStartX = (parent.getWidth() - indicatorTotalWidth) / 2f;

        // vertically centered
        float indicatorPositionY = parent.getHeight() - indicatorHeight / 2f;

        drawInactiveDots(canvas, indicatorStartX, indicatorPositionY, itemCount);


        int activePosition;

        if (parent.getLayoutManager() instanceof GridLayoutManager) {
            activePosition = ((GridLayoutManager) parent.getLayoutManager()).findFirstVisibleItemPosition();
        } else if (parent.getLayoutManager() instanceof LinearLayoutManager) {
            activePosition = ((LinearLayoutManager) parent.getLayoutManager()).findFirstVisibleItemPosition();
        } else {
            // ignore
            return;
        }

        if (activePosition == RecyclerView.NO_POSITION) {
            return;
        }
        if (rtl) {
            activePosition = itemCount - activePosition - 1;
        }

        drawActiveDot(canvas, indicatorStartX, indicatorPositionY, activePosition);
    }

    private void drawInactiveDots(@NonNull Canvas canvas, float indicatorStartX, float indicatorPositionY,
                                  int itemCount) {
        final float itemWidth = this.radius * 2 + indicatorItemPadding;

        float start = indicatorStartX + radius;
        for (int i = 0; i < itemCount; i++) {
            canvas.drawCircle(start, indicatorPositionY, radius, inactivePaint);
            start += itemWidth;
        }
    }

    private void drawActiveDot(@NonNull Canvas canvas, float indicatorStartX, float indicatorPositionY,
                               int highlightPosition) {
        final float itemWidth = this.radius * 2 + indicatorItemPadding;
        float highlightStart = indicatorStartX + radius + itemWidth * highlightPosition;
        canvas.drawCircle(highlightStart, indicatorPositionY, radius, activePaint);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.bottom = indicatorHeight;
    }
}
