package org.wikipedia.gallery;

import android.support.v4.view.ViewPager;
import android.view.View;

public class GalleryPagerTransformer implements ViewPager.PageTransformer {
    private static final float MIN_SCALE = 0.9f;

    @Override
    public void transformPage(View view, float position) {
        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.setTranslationX(0);
        } else if (position <= 0) { // [-1,0]
            float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);
            // fade out
            view.setAlpha(1 + position);
            // keep it in place (undo the default translation)
            view.setTranslationX(-(view.getWidth() * position));
        } else if (position <= 1) { // (0,1]
            // don't do anything to it when it's sliding in.
        } else { // (1,+Infinity]
            // This page is way off-screen to the right.
            view.setTranslationX(0);
        }
    }
}
