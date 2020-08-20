package org.wikipedia.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class LinearLayoutTouchIntercept extends LinearLayout {
    private OnTouchListener onTouchListener;

    public LinearLayoutTouchIntercept(Context context) {
        super(context);
    }

    public LinearLayoutTouchIntercept(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinearLayoutTouchIntercept(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnInterceptTouchListener(OnTouchListener listener) {
        onTouchListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (onTouchListener != null) {
            onTouchListener.onTouch(this, ev);
        }
        return super.onInterceptTouchEvent(ev);
    }
}
