package android.support.v4.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import org.wikipedia.util.log.L;

/**
 * TODO: Remove this class when the Support library and/or PhotoView is updated.
 * This solves an intermittent crash when using ViewPager with the PhotoView component.
 *
 * https://code.google.com/p/android/issues/detail?id=18990
 * https://github.com/chrisbanes/PhotoView/issues/206
 *
 */
public class FixedViewPager extends ViewPager {
    public FixedViewPager(Context context) {
        super(context);
    }

    public FixedViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            L.logRemoteErrorIfProd(e);
            return false;
        }
    }
}
