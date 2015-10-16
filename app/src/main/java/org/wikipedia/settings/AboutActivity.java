package org.wikipedia.settings;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.activity.ActivityUtil;
import org.wikipedia.activity.ThemedActionBarActivity;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.FeedbackUtil;

public class AboutActivity extends ThemedActionBarActivity {
    private static final String KEY_SCROLL_X = "KEY_SCROLL_X";
    private static final String KEY_SCROLL_Y = "KEY_SCROLL_Y";

    private ScrollView mScrollView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        mScrollView = (ScrollView) findViewById(R.id.about_scrollview);
        ((TextView) findViewById(R.id.about_translators)).setText(Html.fromHtml(getString(R.string.about_translators_translatewiki)));
        ((TextView) findViewById(R.id.about_wmf)).setText(Html.fromHtml(getString(R.string.about_wmf)));
        ((TextView) findViewById(R.id.about_version_text)).setText(BuildConfig.VERSION_NAME);
        ((TextView) findViewById(R.id.send_feedback_text)).setText(Html.fromHtml(
                "<a href=\"mailto:mobile-android-wikipedia@wikimedia.org?subject=Android App "
                + BuildConfig.VERSION_NAME
                + " Feedback\">"
                + getString(R.string.send_feedback)
                + "</a>"));

        findViewById(R.id.about_logo_image).setOnClickListener(new AboutLogoClickListener(this));

        //if there's no Email app, hide the Feedback link.
        if (!Utils.mailAppExists(this)) {
            findViewById(R.id.send_feedback_text).setVisibility(View.GONE);
        }

        WikipediaApp.getInstance().adjustDrawableToTheme(((ImageView) findViewById(R.id.about_logo_image)).getDrawable());

        makeEverythingClickable((ViewGroup) findViewById(R.id.about_container));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return ActivityUtil.defaultOnOptionsItemSelected(this, item)
                || super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_SCROLL_X, mScrollView.getScrollX());
        outState.putInt(KEY_SCROLL_Y, mScrollView.getScrollY());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        final int x = savedInstanceState.getInt(KEY_SCROLL_X);
        final int y = savedInstanceState.getInt(KEY_SCROLL_Y);
        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.scrollTo(x, y);
            }
        });
    }

    @Override
    protected void setTheme() {
        setActionBarTheme();
    }

    private void makeEverythingClickable(ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            if (vg.getChildAt(i) instanceof ViewGroup) {
                makeEverythingClickable((ViewGroup)vg.getChildAt(i));
            } else if (vg.getChildAt(i) instanceof TextView) {
                TextView tv = (TextView) vg.getChildAt(i);
                tv.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

    private static class AboutLogoClickListener implements View.OnClickListener {
        private static final int SECRET_CLICK_LIMIT = 7;

        private final Activity mActivity;
        private int mSecretClickCount;

        public AboutLogoClickListener(Activity activity) {
            mActivity = activity;
        }

        @Override
        public void onClick(View v) {
            ++mSecretClickCount;
            if (isSecretClickLimitMet()) {
                if (Prefs.isShowDeveloperSettingsEnabled()) {
                    showSettingAlreadyEnabledMessage();
                } else {
                    Prefs.setShowDeveloperSettingsEnabled(true);
                    showSettingEnabledMessage();
                }
            }
        }

        private boolean isSecretClickLimitMet() {
            return mSecretClickCount == SECRET_CLICK_LIMIT;
        }

        private void showSettingEnabledMessage() {
            FeedbackUtil.showMessage(mActivity, R.string.show_developer_settings_enabled);
        }

        private void showSettingAlreadyEnabledMessage() {
            FeedbackUtil.showMessage(mActivity,
                    R.string.show_developer_settings_already_enabled);
        }
    }
}
