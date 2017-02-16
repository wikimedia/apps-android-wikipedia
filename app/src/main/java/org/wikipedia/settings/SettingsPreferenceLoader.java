package org.wikipedia.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

import static org.apache.commons.lang3.StringUtils.defaultString;

/** UI code for app settings used by PreferenceFragment. */
public class SettingsPreferenceLoader extends BasePreferenceLoader {

    /*package*/ SettingsPreferenceLoader(@NonNull PreferenceFragmentCompat fragment) {
        super(fragment);
    }

    @Override
    public void loadPreferences() {
        loadPreferences(R.xml.preferences);

        if (!Prefs.isZeroTutorialEnabled()) {
            loadPreferences(R.xml.preferences_zero);

            findPreference(R.string.preference_key_zero_interstitial)
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue == Boolean.FALSE) {
                        WikipediaApp.getInstance().getWikipediaZeroHandler().getZeroFunnel().logExtLinkAlways();
                    }
                    return true;
                }
            });
        }

        loadPreferences(R.xml.preferences_about);

        updateLanguagePrefSummary();

        findPreference(R.string.preference_key_language)
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LanguagePreferenceDialog langPrefDialog = new LanguagePreferenceDialog(getActivity(), false);
                langPrefDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        String name = defaultString(WikipediaApp.getInstance().getAppOrSystemLanguageLocalizedName());
                        if (getActivity() != null && !findPreference(R.string.preference_key_language).getSummary().equals(name)) {
                            findPreference(R.string.preference_key_language).setSummary(name);
                            getActivity().setResult(SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED);
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
                intent.setClass(getActivity(), AboutActivity.class);
                getActivity().startActivity(intent);
                return true;
            }
        });
    }

    private void updateLanguagePrefSummary() {
        Preference languagePref = findPreference(R.string.preference_key_language);
        languagePref.setSummary(WikipediaApp.getInstance().getAppOrSystemLanguageLocalizedName());
    }
}
