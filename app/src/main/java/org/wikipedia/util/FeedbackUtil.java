package org.wikipedia.util;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;

import org.wikipedia.R;
import org.wikipedia.main.MainActivity;
import org.wikipedia.page.PageActivity;

import java.util.concurrent.TimeUnit;

import static org.wikipedia.util.UriUtil.visitInExternalBrowser;

public final class FeedbackUtil {
    public static final int LENGTH_DEFAULT = (int) TimeUnit.SECONDS.toMillis(5);
    private static final int SNACKBAR_MAX_LINES = 5;
    private static View.OnLongClickListener TOOLBAR_LONG_CLICK_LISTENER = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            showToolbarButtonToast(v);
            return true;
        }
    };

    public static Snackbar makeSnackbar(Activity activity, CharSequence text, int duration) {
        return makeSnackbar(findBestView(activity), text, duration);
    }

    public static void showError(View containerView, Throwable e) {
        ThrowableUtil.AppError error = ThrowableUtil.getAppError(containerView.getContext(), e);
        makeSnackbar(containerView, error.getError(), LENGTH_DEFAULT).show();
    }

    public static void showMessageAsPlainText(Activity activity, CharSequence possibleHtml) {
        CharSequence richText = StringUtil.fromHtml(possibleHtml.toString());
        showMessage(activity, richText.toString());
    }

    public static void showMessage(Fragment fragment, @StringRes int text) {
        makeSnackbar(fragment.getActivity(), fragment.getString(text), Snackbar.LENGTH_LONG).show();
    }

    public static void showMessage(Fragment fragment, @NonNull String text) {
        makeSnackbar(fragment.getActivity(), text, Snackbar.LENGTH_LONG).show();
    }

    private static void showMessage(View containerView, CharSequence text, int duration) {
        makeSnackbar(containerView, text, duration).show();
    }

    public static void showMessage(Activity activity, @StringRes int resId) {
        showMessage(activity, activity.getString(resId), Snackbar.LENGTH_LONG);
    }

    public static void showMessage(Activity activity, CharSequence text) {
        showMessage(findBestView(activity), text, Snackbar.LENGTH_LONG);
    }

    public static void showMessage(Activity activity, @StringRes int resId, int duration) {
        showMessage(activity, activity.getString(resId), duration);
    }

    public static void showMessage(Activity activity, CharSequence text, int duration) {
        showMessage(findBestView(activity), text, duration);
    }

    public static void showError(Activity activity, Throwable e) {
        showError(findBestView(activity), e);
    }

    public static void showPrivacyPolicy(Context context) {
        visitInExternalBrowser(context, Uri.parse(context.getString(R.string.privacy_policy_url)));
    }

    public static void setToolbarButtonLongPressToast(View... views) {
        for (View v : views) {
            v.setOnLongClickListener(TOOLBAR_LONG_CLICK_LISTENER);
        }
    }

    public static void showTapTargetView(@NonNull Activity activity, @NonNull View target,
                                         @StringRes int titleId, @StringRes int descriptionId,
                                         @Nullable TapTargetView.Listener listener) {
        final float tooltipAlpha = 0.9f;
        TapTargetView.showFor(activity,
                TapTarget.forView(target, activity.getString(titleId),
                        activity.getString(descriptionId))
                        .targetCircleColor(ResourceUtil.getThemedAttributeId(activity, R.attr.colorAccent))
                        .outerCircleColor(ResourceUtil.getThemedAttributeId(activity, R.attr.colorAccent))
                        .outerCircleAlpha(tooltipAlpha)
                        .cancelable(true)
                        .transparentTarget(true),
                listener);
    }

    private static Snackbar makeSnackbar(View view, CharSequence text, int duration) {
        Snackbar snackbar = Snackbar.make(view, text, duration);
        TextView textView = snackbar.getView().findViewById(R.id.snackbar_text);
        textView.setMaxLines(SNACKBAR_MAX_LINES);
        TextView actionView = snackbar.getView().findViewById(R.id.snackbar_action);
        actionView.setTextColor(ContextCompat.getColor(view.getContext(), R.color.green50));
        return snackbar;
    }

    private static void showToolbarButtonToast(View view) {
        Toast toast = Toast.makeText(view.getContext(), view.getContentDescription(), Toast.LENGTH_SHORT);
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        toast.setGravity(Gravity.TOP | Gravity.START, location[0], location[1]);
        toast.show();
    }

    private static View findBestView(Activity activity) {
        if (activity instanceof MainActivity) {
            return activity.findViewById(R.id.fragment_main_coordinator);
        } else if (activity instanceof PageActivity) {
            return activity.findViewById(R.id.page_contents_container);
        } else {
            return activity.findViewById(android.R.id.content);
        }
    }

    private FeedbackUtil() {
    }
}
