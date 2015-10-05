package org.wikipedia.page.gallery;

import android.support.v4.view.ViewPager;
import android.view.View;

public class GalleryPagerTransformer implements ViewPager.PageTransformer {
    private static final float MIN_SCALE = 0.5f;
    private static final float ROTATION_DEGREES = 30.0f;

    public void transformPage(View view, float position) {
        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
        } else if (position <= 0) { // [-1,0]
            float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);
            // fade out
            view.setAlpha(1 + position);
            // give it a bit of a rotation
            view.setRotationY(-position * ROTATION_DEGREES);
        } else if (position <= 1) { // (0,1]
            float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);
            // fade out
            view.setAlpha(1 - position);
            // give it a bit of a rotation
            view.setRotationY(-position * ROTATION_DEGREES);
        } else { // (1,+Infinity]
            // This page is way off-screen to the right.
        }
    }
}