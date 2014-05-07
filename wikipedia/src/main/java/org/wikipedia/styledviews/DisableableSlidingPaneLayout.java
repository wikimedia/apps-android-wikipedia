package org.wikipedia.styledviews;

import android.content.*;
import android.support.v4.widget.*;
import android.util.AttributeSet;
import android.view.*;

public class DisableableSlidingPaneLayout extends SlidingPaneLayout {

    public boolean getSlidingEnabled() {
        if (this.getChildCount() > 0) {
            return this.getChildAt(0).getVisibility() == View.VISIBLE;
        }
        return false;
    }

    public void setSlidingEnabled(boolean enable) {
        if (this.getChildCount() > 0) {
            this.getChildAt(0).setVisibility(enable ? View.VISIBLE : View.GONE);
        }
    }

    public DisableableSlidingPaneLayout(Context context) {
        super(context);
    }

    public DisableableSlidingPaneLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DisableableSlidingPaneLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
