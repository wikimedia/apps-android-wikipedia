package org.wikipedia.views;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import org.wikipedia.util.DimenUtil;

public class PageScrollerView extends AppCompatImageView {
    public interface Callback {
        void onClick();
        void onScrollStart();
        void onScrollStop();
        void onVerticalScroll(float dy);
        void onSwipeOut();
    }

    private static final float SLIDE_OUT_SLOP_WIDTH = 64f * DimenUtil.getDensityScalar();
    private static final int CLICK_MILLIS = 250;

    private boolean dragging;
    private float startX;
    private float startY;
    private float prevX;
    private float prevY;
    private long startMillis;
    @Nullable Callback callback;

    public PageScrollerView(Context context) {
        super(context);
    }

    public PageScrollerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PageScrollerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getRawX();
                startY = event.getRawY();
                prevX = startX;
                prevY = startY;
                if (!dragging) {
                    dragging = true;
                    if (callback != null) {
                        callback.onScrollStart();
                    }
                }
                startMillis = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_UP:
                if (dragging) {
                    dragging = false;
                    if (System.currentTimeMillis() - startMillis < CLICK_MILLIS) {
                        if (callback != null) {
                            callback.onClick();
                        }
                        // TODO: enable if we want the swipe-out action.
                        //} else if (Math.abs(event.getRawX() - startX) > SLIDE_OUT_SLOP_WIDTH) {
                        //    if (callback != null) {
                        //        callback.onSwipeOut();
                        //    }
                    } else if (callback != null) {
                        callback.onScrollStop();
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    dragging = false;
                    if (callback != null) {
                        callback.onScrollStop();
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    float dx = event.getRawX() - prevX;
                    float dy = event.getRawY() - prevY;

                    if (callback != null) {
                        callback.onVerticalScroll(dy);
                    }

                    prevX = event.getRawX();
                    prevY = event.getRawY();
                }
                break;
            default:
                // Do nothing for all the other things
                break;
        }
        return super.onTouchEvent(event);
    }
}
