package org.wikipedia.views;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
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

    /**
     * TODO: Remove this override when it's fixed in support-v4.
     * https://phabricator.wikimedia.org/T88904
     *
     * This seems to have been fixed in the Support library, but not released yet:
     * https://github.com/android/platform_frameworks_support/commit/07a4db40e79aae23694b205f99b013ee2e4f2307
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            return super.onTouchEvent(event);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
