package android.support.v4.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * TODO: Remove this class when Google updates the Support library.
 * This solves an intermittent crash when using DrawerLayout.
 *
 * https://code.google.com/p/android/issues/detail?id=77926
 *
 */
public class FixedDrawerLayout extends DrawerLayout {
    public FixedDrawerLayout(Context context) {
        super(context);
    }

    public FixedDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    boolean isContentView(View child) {
        if (child == null) {
            return false;
        }
        return super.isContentView(child);
    }
}
