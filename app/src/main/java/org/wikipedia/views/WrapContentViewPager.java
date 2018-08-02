package org.wikipedia.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.duolingo.open.rtlviewpager.RtlViewPager;

public class WrapContentViewPager extends RtlViewPager {
    private int maxHeight = Integer.MAX_VALUE;

    public WrapContentViewPager(Context context) {
        super(context);
    }

    public WrapContentViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setMaxHeight(int height) {
        maxHeight = height;
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {
            int height = 0;
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                int h = child.getMeasuredHeight();
                if (h > height) {
                    height = h;
                }
            }
            if (height != 0) {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(Math.min(height, maxHeight), MeasureSpec.EXACTLY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
