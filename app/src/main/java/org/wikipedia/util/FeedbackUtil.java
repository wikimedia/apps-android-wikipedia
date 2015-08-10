package org.wikipedia.util;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.page.PageActivity;

import java.util.concurrent.TimeUnit;

public final class FeedbackUtil {
    public static final int LENGTH_DEFAULT = (int) TimeUnit.SECONDS.toMillis(5);
    private static final int SNACKBAR_MAX_LINES = 5;

    public static Snackbar makeSnackbar(View view, CharSequence text, int duration) {
        Snackbar snackbar = Snackbar.make(view, text, duration);
        TextView textView = (TextView) snackbar.getView().findViewById(R.id.snackbar_text);
        textView.setMaxLines(SNACKBAR_MAX_LINES);
        return snackbar;
    }

    public static void showError(View containerView, Throwable e) {
        ThrowableUtil.AppError error = ThrowableUtil.getAppError(containerView.getContext(), e);
        makeSnackbar(containerView, error.getError(), LENGTH_DEFAULT).show();
    }

    private static void showMessage(View containerView, CharSequence text, int duration) {
        makeSnackbar(containerView, text, duration).show();
    }

    public static void showMessage(Activity activity, int resId) {
        showMessage(activity, activity.getString(resId), Snackbar.LENGTH_LONG);
    }

    public static void showMessage(Activity activity, CharSequence text) {
        showMessage(findBestView(activity), text, Snackbar.LENGTH_LONG);
    }

    public static void showMessage(Activity activity, int resId, int duration) {
        showMessage(activity, activity.getString(resId), duration);
    }

    public static void showMessage(Activity activity, CharSequence text, int duration) {
        showMessage(findBestView(activity), text, duration);
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