package org.wikipedia.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.StringUtil;

/** UI code for app settings used by PreferenceFragment. */
public class SettingsPreferenceLoader extends BasePreferenceLoader {
    private final Activity activity;

    /*package*/ SettingsPreferenceLoader(@NonNull PreferenceFragment fragment) {
        super(fragment);
        activity = fragment.getActivity();
    }

    @Override
    public void loadPreferences() {
        loadPreferences(R.xml.preferences);

        updateLanguagePrefSummary();

        Preference languagePref = findPreference(R.string.preference_key_language);
        languagePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LanguagePreferenceDialog langPrefDialog = new LanguagePreferenceDialog(activity, false);
                langPrefDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        String name = StringUtil.emptyIfNull(WikipediaApp.getInstance().getAppOrSystemLanguageLocalizedName());
                        if (!findPreference(R.string.preference_key_language).getSummary().equals(name)) {
                            findPreference(R.string.preference_key_language).setSummary(name);
                            activity.setResult(SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED);
                        }
                    }
                });
                langPrefDialog.show();
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
        Preference aboutPref = findPreference("about");
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
        Preference languagePref = findPreference(R.string.preference_key_language);
        languagePref.setSummary(WikipediaApp.getInstance().getAppOrSystemLanguageLocalizedName());
    }

    private String getString(@StringRes int id) {
        return activity.getString(id);
    }
}
