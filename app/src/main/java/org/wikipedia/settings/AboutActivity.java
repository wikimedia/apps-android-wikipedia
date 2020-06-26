package org.wikipedia.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.databinding.ActivityAboutBinding;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.StringUtil;

import static org.wikipedia.util.DeviceUtil.mailAppExists;

public class AboutActivity extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActivityAboutBinding binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.sendFeedbackText.setOnClickListener(v -> {
            Intent intent = new Intent()
                    .setAction(Intent.ACTION_SENDTO)
                    .setData(Uri.parse("mailto:android-support@wikimedia.org?subject=Android App "
                            + BuildConfig.VERSION_NAME + " Feedback"));
            startActivity(intent);
        });

        binding.aboutContributors.setText(StringUtil.fromHtml(getString(R.string.about_contributors)));
        RichTextUtil.removeUnderlinesFromLinks(binding.aboutContributors);
        binding.aboutTranslators.setText(StringUtil.fromHtml(getString(R.string.about_translators_translatewiki)));
        RichTextUtil.removeUnderlinesFromLinks(binding.aboutTranslators);
        binding.aboutWmf.setText(StringUtil.fromHtml(getString(R.string.about_wmf)));
        RichTextUtil.removeUnderlinesFromLinks(binding.aboutWmf);
        binding.aboutAppLicense.setText(StringUtil.fromHtml(getString(R.string.about_app_license)));
        RichTextUtil.removeUnderlinesFromLinks(binding.aboutAppLicense);
        binding.aboutVersionText.setText(BuildConfig.VERSION_NAME);
        RichTextUtil.removeUnderlinesFromLinks(binding.activityAboutLibraries);

        binding.aboutLogoImage.setOnClickListener(new AboutLogoClickListener());

        //if there's no Email app, hide the Feedback link.
        if (!mailAppExists(this)) {
            binding.sendFeedbackText.setVisibility(View.GONE);
        }

        makeEverythingClickable(findViewById(R.id.about_container));
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
