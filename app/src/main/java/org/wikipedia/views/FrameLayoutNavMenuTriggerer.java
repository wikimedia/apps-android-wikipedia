package org.wikipedia.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.L10nUtil;

public class FrameLayoutNavMenuTriggerer extends FrameLayout {
    public interface Callback {
        void onNavMenuSwipeRequest(int gravity);
    }

    private static final int SWIPE_SLOP_Y = DimenUtil.roundedDpToPx(32);
    private static final int SWIPE_SLOP_X = DimenUtil.roundedDpToPx(100);
    private static boolean CHILD_VIEW_SCROLLED = false;

    private float initialX;
    private float initialY;
    private boolean maybeSwiping;
    private Callback callback;

    public static void setChildViewScrolled() {
        CHILD_VIEW_SCROLLED = true;
    }

    public FrameLayoutNavMenuTriggerer(Context context) {
        super(context);
    }

    public FrameLayoutNavMenuTriggerer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FrameLayoutNavMenuTriggerer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();

        if (CHILD_VIEW_SCROLLED) {
            CHILD_VIEW_SCROLLED = false;
            initialX = ev.getX();
            initialY = ev.getY();
        }

        if (action == MotionEvent.ACTION_DOWN) {
            initialX = ev.getX();
            initialY = ev.getY();
            maybeSwiping = true;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            maybeSwiping = false;
        } else if (action == MotionEvent.ACTION_MOVE && maybeSwiping) {
            if (Math.abs((int)(ev.getY() - initialY)) > SWIPE_SLOP_Y) {
                maybeSwiping = false;
            } else if (Math.abs(ev.getX() - initialX) > SWIPE_SLOP_X) {
                maybeSwiping = false;
                if (callback != null) {
                    // send an explicit event to children to cancel the current gesture that
                    // they thought was occurring.
                    MotionEvent moveEvent = MotionEvent.obtain(ev);
                    moveEvent.setAction(MotionEvent.ACTION_CANCEL);
                    post(() -> super.dispatchTouchEvent(moveEvent));

                    // and trigger our custom swipe request!
                    callback.onNavMenuSwipeRequest(L10nUtil.isDeviceRTL()
                            ? (ev.getX() > initialX ? Gravity.END : Gravity.START)
                            : (ev.getX() > initialX ? Gravity.START : Gravity.END));
                }
            }
        }

        return false;
    }
}
