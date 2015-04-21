package org.wikipedia.util;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

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

    private static float getDensityScalar() {
        return getDisplayMetrics().density;
    }

    private static DisplayMetrics getDisplayMetrics() {
        return Resources.getSystem().getDisplayMetrics();
    }

    private DimenUtil() {
    }
}