package org.wikipedia.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

public class WikiListView extends ListView {
    private int lastEventX;
    private int lastEventY;

    public WikiListView(Context context) {
        this(context, null);
    }

    public WikiListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WikiListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    public boolean onInterceptTouchEvent(@Nullable MotionEvent ev) {
        if (ev != null) {
            lastEventX = (int) ev.getX();
            lastEventY = (int) ev.getY();
        }
        return false;
    }

    public int getLastEventX() {
        return lastEventX;
    }

    public int getLastEventY() {
        return lastEventY;
    }
}