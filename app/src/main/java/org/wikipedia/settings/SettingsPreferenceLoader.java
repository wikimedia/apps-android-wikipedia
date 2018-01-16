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

        // TODO: remove when reading list syncing is ready for beta/prod.
        if (!ReleaseUtil.isPreBetaRelease()) {
            findPreference(R.string.preference_category_storage_sync).setVisible(false);
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

        Preference offlineLibPref = findPreference(R.string.preference_key_enable_offline_library);
        offlineLibPref.setOnPreferenceChangeListener(new OfflineLibraryEnableListener());
        offlineLibPref.setSummary(StringUtil.fromHtml(getPreferenceHost().getString(R.string.preference_summary_enable_offline_library)));

        loadPreferences(R.xml.preferences_about);

        updateLanguagePrefSummary();

        Preference contentLanguagePref = findPreference(R.string.preference_key_language);

        contentLanguagePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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
            if (newValue == Boolean.TRUE) {
                ((SwitchPreferenceCompat) preference).setChecked(true);
                Prefs.setReadingListSyncEnabled(true);
                Prefs.setReadingListsRemoteSetupPending(true);
                Prefs.setReadingListsRemoteDeletePending(false);
                ReadingListSyncAdapter.manualSync();
            } else {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.reading_lists_confirm_remote_delete)
                        .setPositiveButton(R.string.reading_lists_confirm_remote_delete_yes, new DeleteRemoteListsYesListener(preference))
                        .setNegativeButton(R.string.reading_lists_confirm_remote_delete_no, new DeleteRemoteListsNoListener(preference))
                        .show();
            }
            // clicks are handled and preferences updated accordingly; don't pass the result through
            return false;
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

    private static final class DeleteRemoteListsYesListener implements DialogInterface.OnClickListener {
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

    private static final class DeleteRemoteListsNoListener implements DialogInterface.OnClickListener {
        private Preference preference;

        private DeleteRemoteListsNoListener(Preference preference) {
            this.preference = preference;
        }

        @Override public void onClick(DialogInterface dialog, int which) {
            ((SwitchPreferenceCompat) preference).setChecked(true);
            Prefs.setReadingListSyncEnabled(true);
            Prefs.setReadingListsRemoteSetupPending(true);
            Prefs.setReadingListsRemoteDeletePending(false);
            ReadingListSyncAdapter.manualSync();
        }
    }
}
