package androidx.viewpager.widget;

import android.content.Context;
import android.util.AttributeSet;

public class ViewPagerWithVelocity extends ViewPager {
    private static final int VELOCITY = 1000;

    public ViewPagerWithVelocity(Context context) {
        super(context);
    }

    public ViewPagerWithVelocity(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    void setCurrentItemInternal(int item, boolean smoothScroll, boolean always) {
        setCurrentItemInternal(item, smoothScroll, always, VELOCITY);
    }
}
