package org.wikipedia.views;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewManager;

import org.wikipedia.util.ApiUtil;

public final class ViewUtil {
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

    @Nullable public static <T extends View> T findView(@NonNull View view, @IdRes int id) {
        //noinspection unchecked
        return (T) view.findViewById(id);
    }

    @Nullable public static <T extends View> T findView(@NonNull Activity activity, @IdRes int id) {
        //noinspection unchecked
        return (T) activity.findViewById(id);
    }

    private ViewUtil() { }
}
