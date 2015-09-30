package org.wikipedia.util;

import android.content.res.Resources;
import android.support.annotation.DimenRes;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import org.wikipedia.WikipediaApp;

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

    public static float getFloat(@DimenRes int id) {
        return getValue(id).getFloat();
    }

    /** @return Dimension in dp. */
    public static float getDimension(@DimenRes int id) {
        return TypedValue.complexToFloat(getValue(id).data);
    }

    private static TypedValue getValue(@DimenRes int id) {
        TypedValue typedValue = new TypedValue();
        getResources().getValue(id, typedValue, true);
        return typedValue;
    }

    private static DisplayMetrics getDisplayMetrics() {
        return getResources().getDisplayMetrics();
    }

    private static Resources getResources() {
        return WikipediaApp.getInstance().getResources();
    }

    /**
     * Gets the screen height in pixels.
     * @return screen height in pixels
     */
    public static int getDisplayHeightPx() {
        return getDisplayMetrics().heightPixels;
    }

    private DimenUtil() { }
}