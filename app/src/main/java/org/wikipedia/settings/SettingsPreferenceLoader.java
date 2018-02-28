package org.wikipedia.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter;
import org.wikipedia.theme.ThemeFittingRoomActivity;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.StringUtil;

import static org.apache.commons.lang3.StringUtils.defaultString;

/** UI code for app settings used by PreferenceFragment. */
class SettingsPreferenceLoader extends BasePreferenceLoader {

    /*package*/ SettingsPreferenceLoader(@NonNull PreferenceFragmentCompat fragment) {
        super(fragment);
    }

    @Override
    public void loadPreferences() {
        loadPreferences(R.xml.preferences);

        if (ReadingListSyncAdapter.isDisabledByRemoteConfig()) {
            findPreference(R.string.preference_category_sync).setVisible(false);
            findPreference(R.string.preference_key_sync_reading_lists).setVisible(false);
        }

        if (!Prefs.isZeroTutorialEnabled()) {
            loadPreferences(R.xml.preferences_zero);

            findPreference(R.string.preference_key_zero_interstitial)
                    .setOnPreferenceChangeListener(new ShowZeroInterstitialListener());
        }

        findPreference(R.string.preference_key_sync_reading_lists)
                .setOnPreferenceChangeListener(new SyncReadingListsListener());

        Preference eventLoggingOptInPref = findPreference(R.string.preference_key_eventlogging_opt_in);
        eventLoggingOptInPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!((boolean) newValue)) {
                Prefs.setAppInstallId(null);
            }
            return true;
        });

        if (ReleaseUtil.isPreBetaRelease()) {
            loadPreferences(R.xml.preferences_experimental);
            Preference offlineLibPref = findPreference(R.string.preference_key_enable_offline_library);
            offlineLibPref.setOnPreferenceChangeListener(new OfflineLibraryEnableListener());
            offlineLibPref.setSummary(StringUtil.fromHtml(getPreferenceHost().getString(R.string.preference_summary_enable_offline_library)));
        }

        loadPreferences(R.xml.preferences_about);

        updateLanguagePrefSummary();

        Preference contentLanguagePref = findPreference(R.string.preference_key_language);

        contentLanguagePref.setOnPreferenceClickListener((preference) -> {
            LanguagePreferenceDialog langPrefDialog = new LanguagePreferenceDialog(getActivity(), false);
            langPrefDialog.setOnDismissListener((dialog) -> {
                String name = defaultString(WikipediaApp.getInstance().getAppOrSystemLanguageLocalizedName());
                if (getActivity() != null && !findPreference(R.string.preference_key_language).getSummary().equals(name)) {
                    findPreference(R.string.preference_key_language).setSummary(name);
                    getActivity().setResult(SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED);
                }
            });
            langPrefDialog.show();
            return true;
        });

        Preference themePref = findPreference(R.string.preference_key_color_theme);
        themePref.setSummary(WikipediaApp.getInstance().getCurrentTheme().getNameId());
        themePref.setOnPreferenceClickListener(preference -> {
            getActivity().startActivity(ThemeFittingRoomActivity.newIntent(getActivity()));
            return true;
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
        aboutPref.setOnPreferenceClickListener((preference) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClass(getActivity(), AboutActivity.class);
            getActivity().startActivity(intent);
            return true;
        });
    }

    private void updateLanguagePrefSummary() {
        Preference languagePref = findPreference(R.string.preference_key_language);
        languagePref.setSummary(WikipediaApp.getInstance().getAppOrSystemLanguageLocalizedName());
    }

    private static class ShowZeroInterstitialListener implements Preference.OnPreferenceChangeListener {
        @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (newValue == Boolean.FALSE) {
                WikipediaApp.getInstance().getWikipediaZeroHandler().getZeroFunnel().logExtLinkAlways();
            }
            return true;
        }
    }

    private final class SyncReadingListsListener implements Preference.OnPreferenceChangeListener {
        @Override public boolean onPreferenceChange(final Preference preference, Object newValue) {
            if (AccountUtil.isLoggedIn()) {
                if (newValue == Boolean.TRUE) {
                    ((SwitchPreferenceCompat) preference).setChecked(true);
                    ReadingListSyncAdapter.setSyncEnabledWithSetup();
                } else {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getActivity().getString(R.string.preference_dialog_of_turning_off_reading_list_sync_title, AccountUtil.getUserName()))
                            .setMessage(getActivity().getString(R.string.preference_dialog_of_turning_off_reading_list_sync_text, AccountUtil.getUserName()))
                            .setPositiveButton(R.string.reading_lists_confirm_remote_delete_yes, new DeleteRemoteListsYesListener(preference))
                            .setNegativeButton(R.string.reading_lists_confirm_remote_delete_no, null)
                            .show();
                }
            } else {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.reading_list_preference_login_to_enable_sync_dialog_title)
                        .setMessage(R.string.reading_list_preference_login_to_enable_sync_dialog_text)
                        .setPositiveButton(R.string.reading_list_preference_login_to_enable_sync_dialog_login,
                                (dialogInterface, i) -> {
                                    Intent loginIntent = LoginActivity.newIntent(getActivity(),
                                            LoginFunnel.SOURCE_SETTINGS);

                                    getActivity().startActivity(loginIntent);
                                })
                        .setNegativeButton(R.string.reading_list_preference_login_to_enable_sync_dialog_cancel, null)
                        .show();
            }
            // clicks are handled and preferences updated accordingly; don't pass the result through
            return false;
        }
    }

    public void updateSyncReadingListsPrefSummary() {
        Preference syncReadingListsPref = findPreference(R.string.preference_key_sync_reading_lists);
        if (AccountUtil.isLoggedIn()) {
            syncReadingListsPref.setSummary(getActivity().getString(R.string.preference_summary_sync_reading_lists_from_account, AccountUtil.getUserName()));
        } else {
            syncReadingListsPref.setSummary(R.string.preference_summary_sync_reading_lists);
        }
    }

    private final class OfflineLibraryEnableListener implements Preference.OnPreferenceChangeListener {
        @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (((Boolean) newValue)) {
                ((BaseActivity) getActivity()).searchOfflineCompilationsWithPermission(true);
            }
            return true;
        }
    }

    private final class DeleteRemoteListsYesListener implements DialogInterface.OnClickListener {
        private Preference preference;

        private DeleteRemoteListsYesListener(Preference preference) {
            this.preference = preference;
        }

        @Override public void onClick(DialogInterface dialog, int which) {
            ((SwitchPreferenceCompat) preference).setChecked(false);
            Prefs.setReadingListSyncEnabled(false);
            Prefs.setReadingListsRemoteSetupPending(false);
            Prefs.setReadingListsRemoteDeletePending(true);
            ReadingListSyncAdapter.manualSync();
        }
    }
}
