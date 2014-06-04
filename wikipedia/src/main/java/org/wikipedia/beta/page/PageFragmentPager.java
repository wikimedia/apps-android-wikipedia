package org.wikipedia.beta.page;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class PageFragmentPager extends ViewPager {

    private boolean isPagingEnabled = true;
    private OnAnimationListener onAnimationListener;
    private boolean isAnimating = false;

    public PageFragmentPager(Context context) {
        super(context);
        this.setOnPageChangeListener(new FragmentPagerListener());
    }

    public PageFragmentPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnPageChangeListener(new FragmentPagerListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.isPagingEnabled) {
            return super.onTouchEvent(event);
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.isPagingEnabled) {
            return super.onInterceptTouchEvent(event);
        }
        return false;
    }

    public void setPagingEnabled(boolean b) {
        this.isPagingEnabled = b;
    }

    private class FragmentPagerListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionPixels) {
        }
        private boolean pageChanged = false;
        @Override
        public void onPageSelected(int position) {
            pageChanged = true;
        }
        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                isAnimating = false;
                if (pageChanged) {
                    if (onAnimationListener != null) {
                        onAnimationListener.OnAnimationFinished();
                    }
                }
            } else {
                isAnimating = true;
            }
        }
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    public interface OnAnimationListener {
        public void OnAnimationFinished();
    }

    public void setOnAnimationListener(OnAnimationListener listener) {
        onAnimationListener = listener;
    }
}