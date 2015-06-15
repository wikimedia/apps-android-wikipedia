package org.wikipedia.util;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;

public final class DimenUtil {
    public static float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getDisplayMetrics());
    }

    public static int roundedDpToPx(float dp) {
        return Math.round(dpToPx(dp));
    }

    public static float pxToDp(float px) {
        return px / getDensityScalar();
    }

    public static int roundedPxToDp(float px) {
        return Math.round(pxToDp(px));
    }

    public static float getDensityScalar() {
        return getDisplayMetrics().density;
    }

    private static DisplayMetrics getDisplayMetrics() {
        return Resources.getSystem().getDisplayMetrics();
    }

    /**
     * Gets the screen height in pixels.
     * There is a simpler DimenUtil.getDisplayMetrics()#heightPixels but that doesn't work on 2.3
     * devices since it always returns 0.
     * @param display the screen to measure
     * @return screen height in pixels
     */
    public static int getDisplayHeight(Display display) {
        if (ApiUtil.hasHoneyCombMr2()) {
            return getDisplayHeightNewer(display);
        } else {
            return getDisplayHeightOlder(display);
        }
    }

    private static int getDisplayHeightOlder(Display display) {
        //noinspection deprecation
        return display.getHeight();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private static int getDisplayHeightNewer(Display display) {
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }

    private DimenUtil() {
    }
}