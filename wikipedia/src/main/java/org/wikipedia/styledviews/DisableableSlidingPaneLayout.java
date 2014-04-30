package org.wikipedia.styledviews;

import android.content.*;
import android.support.v4.widget.*;
import android.util.AttributeSet;
import android.view.*;

public class DisableableSlidingPaneLayout extends SlidingPaneLayout {

    private boolean enableSliding = true;
    public boolean getSlidingEnabled() { return enableSliding; }
    public void setSlidingEnabled(boolean enable) { enableSliding = enable; }

    public DisableableSlidingPaneLayout(Context context) {
        super(context);
    }

    public DisableableSlidingPaneLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DisableableSlidingPaneLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // if sliding is disabled, then consume the touch event.
        return enableSliding ? super.onTouchEvent(ev) : true;
    }
}
