package org.wikipedia.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.DimenRes;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import org.wikipedia.R;
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

    /**
     * Calculates the actual font size for the current device, based on an "sp" measurement.
     * @param window The window on which the font will be rendered.
     * @param fontSp Measurement in "sp" units of the font.
     * @return Actual font size for the given sp amount.
     */
    public static float getFontSizeFromSp(Window window, float fontSp) {
        final DisplayMetrics metrics = new DisplayMetrics();
        window.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return fontSp / metrics.scaledDensity;
    }

    // TODO: use getResources().getDimensionPixelSize()?  Define leadImageWidth with px, not dp?
    public static int calculateLeadImageWidth() {
        Resources res = WikipediaApp.getInstance().getResources();
        return (int) (res.getDimension(R.dimen.leadImageWidth) / getDensityScalar());
    }

    public static int getDisplayWidthPx() {
        return getDisplayMetrics().widthPixels;
    }

    public static int getDisplayHeightPx() {
        return getDisplayMetrics().heightPixels;
    }

    public static int getContentTopOffsetPx(Context context) {
        return roundedDpToPx(getContentTopOffset(context));
    }

    public static float getContentTopOffset(Context context) {
        return getToolbarHeight(context);
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


    private static float getStatusBarHeight(Context context) {
        int id = getStatusBarId(context);
        return id > 0 ? DimenUtil.getDimension(id) : 0;
    }

    private static float getToolbarHeight(Context context) {
        return DimenUtil.roundedPxToDp(getToolbarHeightPx(context));
    }

    /**
     * Returns the height of the toolbar in the current activity. The system controls the height of
     * the toolbar, which may be slightly different depending on screen orientation, and device
     * version.
     * @param context Context used for retrieving the height attribute.
     * @return Height of the toolbar.
     */
    private static int getToolbarHeightPx(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(new int[] {
                android.support.v7.appcompat.R.attr.actionBarSize
        });
        int size = styledAttributes.getDimensionPixelSize(0, 0);
        styledAttributes.recycle();
        return size;
    }

    @DimenRes private static int getStatusBarId(Context context) {
        return context.getResources().getIdentifier("status_bar_height", "dimen", "android");
    }

    public static void setViewHeight(View view, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = height;
        view.setLayoutParams(params);
    }

    public static int leadImageHeightForDevice() {
        return (int) (getDisplayHeightPx() * articleHeaderViewScreenHeightRatio());
    }

    private static float articleHeaderViewScreenHeightRatio() {
        return DimenUtil.getFloat(R.dimen.articleHeaderViewScreenHeightRatio);
    }

    private DimenUtil() { }
}
