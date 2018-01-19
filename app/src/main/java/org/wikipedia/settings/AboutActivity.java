package org.wikipedia.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.StringUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.wikipedia.util.DeviceUtil.mailAppExists;

public class AboutActivity extends BaseActivity {
    @BindView(R.id.about_translators) TextView translatorsTextView;
    @BindView(R.id.activity_about_libraries) TextView librariesTextView;
    @BindView(R.id.about_app_license) TextView appLicenseTextView;
    @BindView(R.id.send_feedback_text) TextView feedbackTextView;
    @BindView(R.id.about_wmf) TextView wmfTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);

        translatorsTextView.setText(StringUtil.fromHtml(getString(R.string.about_translators_translatewiki)));
        RichTextUtil.removeUnderlinesFromLinks(translatorsTextView);
        wmfTextView.setText(StringUtil.fromHtml(getString(R.string.about_wmf)));
        RichTextUtil.removeUnderlinesFromLinks(wmfTextView);
        appLicenseTextView.setText(StringUtil.fromHtml(getString(R.string.about_app_license)));
        RichTextUtil.removeUnderlinesFromLinks(appLicenseTextView);
        ((TextView) findViewById(R.id.about_version_text)).setText(BuildConfig.VERSION_NAME);
        RichTextUtil.removeUnderlinesFromLinks(librariesTextView);

        findViewById(R.id.about_logo_image).setOnClickListener(new AboutLogoClickListener());

        //if there's no Email app, hide the Feedback link.
        if (!mailAppExists(this)) {
            feedbackTextView.setVisibility(View.GONE);
        }

        makeEverythingClickable(findViewById(R.id.about_container));
    }

    @OnClick(R.id.send_feedback_text) void onSendFeedbackClick(View v) {
        Intent intent = new Intent()
                .setAction(Intent.ACTION_SENDTO)
                .setData(Uri.parse("mailto:mobile-android-wikipedia@wikimedia.org?subject=Android App "
                        + BuildConfig.VERSION_NAME + " Feedback"));
        startActivity(intent);
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
        private int mSecretClickCount;

        @Override
        public void onClick(View v) {
            ++mSecretClickCount;
            if (isSecretClickLimitMet()) {
                if (Prefs.isShowDeveloperSettingsEnabled()) {
                    showSettingAlreadyEnabledMessage((Activity) v.getContext());
                } else {
                    Prefs.setShowDeveloperSettingsEnabled(true);
                    showSettingEnabledMessage((Activity) v.getContext());
                }
            }
        }

        private boolean isSecretClickLimitMet() {
            return mSecretClickCount == SECRET_CLICK_LIMIT;
        }

        private void showSettingEnabledMessage(@NonNull Activity activity) {
            FeedbackUtil.showMessage(activity, R.string.show_developer_settings_enabled);
        }

        private void showSettingAlreadyEnabledMessage(@NonNull Activity activity) {
            FeedbackUtil.showMessage(activity, R.string.show_developer_settings_already_enabled);
        }
    }
}
