package org.wikipedia.drawable;

import android.annotation.TargetApi;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;

import org.wikipedia.util.ApiUtil;

public final class DrawableUtil {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setTint(Drawable drawable, @ColorInt int color) {
        if (ApiUtil.hasLollipop()) {
            drawable.setTint(color);
        } else {
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }
    }

    private DrawableUtil() { }
}