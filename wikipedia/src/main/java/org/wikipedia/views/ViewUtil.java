package org.wikipedia.views;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.animation.AlphaAnimation;

import org.wikipedia.util.ApiUtil;

public final class ViewUtil {
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void setAlpha(View view, float alpha) {
        if (ApiUtil.hasHoneyComb()) {
            ViewCompat.setAlpha(view, alpha);
        } else {
            ViewUtil.setAlphaDeprecated(view, alpha);
        }
    }

    // http://stackoverflow.com/q/4813995/970346
    public static void setAlphaDeprecated(View view, float alpha) {
        AlphaAnimation animation = new AlphaAnimation(alpha, alpha);
        animation.setDuration(0);
        animation.setFillAfter(true);
        view.startAnimation(animation);
    }

    private ViewUtil() {
    }
}