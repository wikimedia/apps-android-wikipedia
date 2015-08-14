package org.wikipedia.views;

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewManager;
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

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @SuppressWarnings("deprecation")
    public static void setBackgroundDrawable(View view, Drawable drawable) {
        if (ApiUtil.hasJellyBean()) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }
    }

    public static boolean detach(@Nullable View view) {
        if (view != null && view.getParent() instanceof ViewManager) {
            ((ViewManager) view.getParent()).removeView(view);
            return true;
        }
        return false;
    }

    public static void setBottomPaddingDp(View view, int padding) {
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(),
                (int) (padding * view.getContext().getResources().getDisplayMetrics().density));
    }

    private ViewUtil() { }
}
