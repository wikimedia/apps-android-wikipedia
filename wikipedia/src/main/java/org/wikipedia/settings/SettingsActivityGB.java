package org.wikipedia.settings;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.StringUtil;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.annotation.NonNull;
import android.support.v7.internal.view.ContextThemeWrapper;
import android.view.MenuItem;

/**
 * Settings activity that is specifically intended for API 10.
 * It's functionally identical to the real SettingsActivity, except that this one inherits from
 * PreferenceActivity, which was deprecated after API 10. The new SettingsActivity inherits from
 * ActionBarActivity, and uses a PreferenceFragment, all of which are necessary for all the
 * components to render properly (specifically checkboxes).
 */
public class SettingsActivityGB extends PreferenceActivity {

    public void onCreate(Bundle savedInstanceState) {
        setTheme(WikipediaApp.getInstance().getCurrentTheme().getResourceId());
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        final Context context = this;
        updateLanguagePrefSummary();

        Preference languagePref = findPreference(getString(R.string.preference_key_language));
        languagePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LanguagePreferenceDialog langPrefDialog = new LanguagePreferenceDialog(
                        new ContextThemeWrapper(context,
                            (WikipediaApp.getInstance().isCurrentThemeLight() ?  R.style.NoTitle : R.style.NoTitleWikiDark)));
                langPrefDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (!findPreference(getString(R.string.preference_key_language)).getSummary()
                                .equals(WikipediaApp.getInstance().getAppLanguageLocalizedName())) {
                            findPreference(getString(R.string.preference_key_language)).setSummary(
                                    StringUtil.emptyIfNull(WikipediaApp.getInstance().getAppLanguageLocalizedName()));
                            setResult(SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED);
                        }
                    }
                });
                langPrefDialog.show();
                return true;
            }
        });

        Preference logoutPref = findPreference(getString(R.string.preference_key_logout));
        if (!WikipediaApp.getInstance().getUserInfoStorage().isLoggedIn()) {
            logoutPref.setEnabled(false);
            logoutPref.setSummary(getString(R.string.preference_summary_notloggedin));
        }
        logoutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                setResult(SettingsActivity.ACTIVITY_RESULT_LOGOUT);
                finish();
                return false;
            }
        });

        if (!BuildConfig.APPLICATION_ID.equals("org.wikipedia")) {
            overridePackageName();
        }
    }

    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                throw new RuntimeException("WAT");
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
                intent.setClass(SettingsActivityGB.this, AboutActivity.class);
                startActivity(intent);
                return true;
            }
        });
    }

    private void updateLanguagePrefSummary() {
        Preference languagePref = findPreference(getString(R.string.preference_key_language));
        languagePref.setSummary(WikipediaApp.getInstance().getAppLanguageLocalizedName());
    }
}
