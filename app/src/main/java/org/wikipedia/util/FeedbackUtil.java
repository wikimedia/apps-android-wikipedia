package org.wikipedia.util;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;
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
import org.wikipedia.random.RandomActivity;

import java.util.concurrent.TimeUnit;

import static org.wikipedia.util.UriUtil.visitInExternalBrowser;

public final class FeedbackUtil {
    public static final int LENGTH_DEFAULT = (int) TimeUnit.SECONDS.toMillis(5);
    private static final int SNACKBAR_MAX_LINES = 5;
    private static View.OnLongClickListener TOOLBAR_LONG_CLICK_LISTENER = (v) -> {
        showToolbarButtonToast(v);
        return true;
    };

    public static void showError(Activity activity, Throwable e) {
        ThrowableUtil.AppError error = ThrowableUtil.getAppError(activity, e);
        makeSnackbar(activity, error.getError(), LENGTH_DEFAULT).show();
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

    public static void showMessage(Activity activity, @StringRes int resId) {
        showMessage(activity, activity.getString(resId), Snackbar.LENGTH_LONG);
    }

    public static void showMessage(Activity activity, CharSequence text) {
        showMessage(activity, text, Snackbar.LENGTH_LONG);
    }

    public static void showMessage(Activity activity, @StringRes int resId, int duration) {
        showMessage(activity, activity.getString(resId), duration);
    }

    public static void showMessage(Activity activity, CharSequence text, int duration) {
        makeSnackbar(activity, text, duration).show();
    }

    public static void showPrivacyPolicy(Context context) {
        visitInExternalBrowser(context, Uri.parse(context.getString(R.string.privacy_policy_url)));
    }

    public static void showOfflineReadingAndData(Context context) {
        visitInExternalBrowser(context, Uri.parse(context.getString(R.string.offline_reading_and_data_url)));
    }

    public static void showAboutWikipedia(Context context) {
        visitInExternalBrowser(context, Uri.parse(context.getString(R.string.about_wikipedia_url)));
    }

    public static void showAndroidAppFAQ(Context context) {
        visitInExternalBrowser(context, Uri.parse(context.getString(R.string.android_app_faq_url)));
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

    public static Snackbar makeSnackbar(Activity activity, CharSequence text, int duration) {
        View view = findBestView(activity);
        Snackbar snackbar = Snackbar.make(view, text, duration);
        TextView textView = snackbar.getView().findViewById(R.id.snackbar_text);
        textView.setMaxLines(SNACKBAR_MAX_LINES);
        TextView actionView = snackbar.getView().findViewById(R.id.snackbar_action);
        actionView.setTextColor(ContextCompat.getColor(view.getContext(), R.color.green50));
        adjustLayoutParamsIfRequired(snackbar, activity);
        return snackbar;
    }

    private static void adjustLayoutParamsIfRequired(Snackbar snackbar, Activity activity) {
        if (activity instanceof PageActivity) {
            // TODO: move getLayoutParams() out of this logic if there has more special cases
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) snackbar.getView().getLayoutParams();
            int tabLayoutHeight = ((PageActivity) activity).getTabLayout().getHeight();
            params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin + tabLayoutHeight);
            snackbar.getView().setLayoutParams(params);
        }
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
            return activity.findViewById(R.id.fragment_page_coordinator);
        } else if (activity instanceof RandomActivity) {
            return activity.findViewById(R.id.random_coordinator_layout);
        } else {
            return activity.findViewById(android.R.id.content);
        }
    }

    private FeedbackUtil() {
    }
}
