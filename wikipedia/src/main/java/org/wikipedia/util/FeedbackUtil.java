package org.wikipedia.util;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.page.PageActivity;

public final class FeedbackUtil {

    public static void showError(View containerView, Throwable e) {
        ThrowableUtil.AppError error = ThrowableUtil.getAppError(containerView.getContext(), e);
        Snackbar.make(containerView, error.getError(), Snackbar.LENGTH_LONG).show();
    }

    private static void showMessage(View containerView, CharSequence text) {
        Snackbar.make(containerView, text, Snackbar.LENGTH_LONG).show();
    }

    private static void showMessage(View containerView, int resId) {
        showMessage(containerView, containerView.getResources().getString(resId));
    }

    public static void showMessage(Activity activity, int resId) {
        showMessage(findBestView(activity), activity.getString(resId));
    }

    public static void showMessage(Activity activity, CharSequence text) {
        showMessage(findBestView(activity), text);
    }

    public static void showError(Activity activity, Throwable e) {
        showError(findBestView(activity), e);
    }

    private static View findBestView(Activity activity) {
        if (activity instanceof PageActivity
                && ((PageActivity) activity).getCurPageFragment() != null) {
            return activity.findViewById(R.id.page_contents_container);
        } else {
            return activity.findViewById(android.R.id.content);
        }
    }

    private FeedbackUtil() {
    }
}