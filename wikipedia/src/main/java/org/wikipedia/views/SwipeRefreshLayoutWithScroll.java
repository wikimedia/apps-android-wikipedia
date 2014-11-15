package org.wikipedia.views;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.View;

public class SwipeRefreshLayoutWithScroll extends SwipeRefreshLayout {

    public SwipeRefreshLayoutWithScroll(Context context) {
        super(context);
    }

    public SwipeRefreshLayoutWithScroll(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private View scrollableView;
    public void setScrollableChild(View scrollableView) {
        this.scrollableView = scrollableView;
    }

    @Override
    public boolean canChildScrollUp() {
        if (scrollableView == null) {
            return false;
        }
        return scrollableView.getScrollY() > 0;
    }
}
