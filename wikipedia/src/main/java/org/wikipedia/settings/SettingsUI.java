package org.wikipedia.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;
import android.support.v7.internal.view.ContextThemeWrapper;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.StringUtil;

/**
 * UI code for app settings, shared between PreferenceActivity (GB) and PreferenceFragment (HC+).
 */
public class SettingsUI {
    private final Activity activity;
    private final PreferenceHostCompat prefHost;

    public SettingsUI(Activity activity, PreferenceHostCompat prefHost) {
        this.activity = activity;
        this.prefHost = prefHost;
    }

    public void loadPreferences() {
        prefHost.addPreferencesFromResource(R.xml.preferences);

        updateLanguagePrefSummary();

        Preference languagePref = prefHost.findPreference(
                activity.getString(R.string.preference_key_language));
        languagePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LanguagePreferenceDialog langPrefDialog = new LanguagePreferenceDialog(
                        new ContextThemeWrapper(activity,
                                (WikipediaApp.getInstance().isCurrentThemeLight()
                                        ? R.style.NoTitle
                                        : R.style.NoTitleWikiDark)));
                langPrefDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (!prefHost.findPreference(activity.getString(R.string.preference_key_language)).getSummary()
                                .equals(WikipediaApp.getInstance().getAppLanguageLocalizedName())) {
                            prefHost.findPreference(activity.getString(R.string.preference_key_language)).setSummary(
                                    StringUtil.emptyIfNull(WikipediaApp.getInstance().getAppLanguageLocalizedName()));
                            activity.setResult(SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED);
                        }
                    }
                });
                langPrefDialog.show();
                return true;
            }
        });

        Preference logoutPref = prefHost.findPreference(activity.getString(R.string.preference_key_logout));
        if (!WikipediaApp.getInstance().getUserInfoStorage().isLoggedIn()) {
            logoutPref.setEnabled(false);
            logoutPref.setSummary(activity.getString(R.string.preference_summary_notloggedin));
        }
        logoutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                activity.setResult(SettingsActivity.ACTIVITY_RESULT_LOGOUT);
                activity.finish();
                return true;
            }
        });

        if (!BuildConfig.APPLICATION_ID.equals("org.wikipedia")) {
            overridePackageName();
        }
    }

    /**
     * Needed for beta release since the Gradle flavors applicationId changes don't get reflected
     * to the preferences.xml
     * See https://code.google.com/p/android/issues/detail?id=57460
     */
    private void overridePackageName() {
        Preference aboutPref = prefHost.findPreference("about");
        aboutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClass(activity, AboutActivity.class);
                activity.startActivity(intent);
                return true;
            }
        });
    }

    private void updateLanguagePrefSummary() {
        Preference languagePref = prefHost.findPreference(activity.getString(R.string.preference_key_language));
        languagePref.setSummary(WikipediaApp.getInstance().getAppLanguageLocalizedName());
    }
}
