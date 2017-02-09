package org.wikipedia.views;

import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class DontInterceptTouchListener implements RecyclerView.OnItemTouchListener {
    private int pointerId = Integer.MIN_VALUE;
    private float x = Float.MIN_VALUE;
    private float y = Float.MIN_VALUE;
    private boolean disallowInterception;

    @Override public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent event) {
        int action = event.getActionMasked();
        switch(action) {
            case MotionEvent.ACTION_DOWN:
                pointerId = event.getPointerId(0);
                x = event.getX();
                y = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (disallowInterception) {
                    break;
                }

                int pointerIndex = event.findPointerIndex(pointerId);
                if (pointerIndex < 0) {
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
                }

                float dy = Math.abs(y - event.getY(pointerIndex));
                float dx = Math.abs(x - event.getX(pointerIndex));
                int slop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();

                if (dx > slop) {
                    disallowInterception = true;
                } else if (dy > slop) {
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
                }

                view.getParent().requestDisallowInterceptTouchEvent(true);
                break;
            default:
                this.pointerId = Integer.MIN_VALUE;
                x = Float.MIN_VALUE;
                y = Float.MIN_VALUE;
                disallowInterception = false;
                break;
        }
        return false;
    }

    @Override public void onTouchEvent(RecyclerView view, MotionEvent event) { }
    @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) { }
}
