package org.wikipedia.settings;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.TwoStatePreference;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.crash.RemoteLogException;
import org.wikipedia.useroption.ui.UserOptionRowActivity;
import org.wikipedia.util.log.L;

import java.util.List;

/*package*/ class DeveloperSettingsPreferenceLoader extends BasePreferenceLoader {
    @NonNull private final Context context;

    @NonNull private final Preference.OnPreferenceChangeListener setRestBaseManuallyChangeListener
            = new Preference.OnPreferenceChangeListener() {
        /**
         * Called when the useRestBaseSetManually preference has been changed by the user. This is
         * called before the state of the Preference is about to be updated and
         * before the state is persisted.
         *
         * @param preference The changed Preference.
         * @param newValue   The new value of the Preference.
         * @return True to update the state of the Preference with the new value.
         */
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            setUseRestBasePreference((Boolean) newValue);
            return true;
        }
    };

    @NonNull private final Preference.OnPreferenceChangeListener setMediaWikiBaseUriChangeListener
            = new Preference.OnPreferenceChangeListener() {
        /**
         * Called when the mediaWikiBaseUri preference has been changed by the user. This is
         * called before the state of the Preference is about to be updated and
         * before the state is persisted.
         *
         * @param preference The changed Preference.
         * @param newValue   The new value of the Preference.
         * @return True to update the state of the Preference with the new value.
         */
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            resetMediaWikiSettings();
            return true;
        }
    };

    @NonNull private final Preference.OnPreferenceChangeListener setMediaWikiMultiLangSupportChangeListener
            = new Preference.OnPreferenceChangeListener() {
        /**
         * Called when the mediaWikiBaseUriSupportsLangCode preference has been changed by the user.
         * This is called before the state of the Preference is about to be updated and
         * before the state is persisted.
         *
         * @param preference The changed Preference.
         * @param newValue   The new value of the Preference.
         * @return True to update the state of the Preference with the new value.
         */
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            resetMediaWikiSettings();
            return true;
        }
    };

    /*package*/
    DeveloperSettingsPreferenceLoader(@NonNull PreferenceFragmentCompat fragment) {
        super(fragment);
        this.context = fragment.getActivity();
    }

    @Override
    public void loadPreferences() {
        loadPreferences(R.xml.developer_preferences);
        setUpRestBaseCheckboxes();
        setUpMediaWikiSettings();
        setUpCookies((PreferenceCategory) findPreference(R.string.preferences_developer_cookies_key));
        setUpCrashButton(findPreference(getCrashButtonKey()));
        setUpUserOptionButton(findPreference(getUserOptionButtonKey()));
        setUpRemoteLogButton(findPreference(R.string.preference_key_remote_log));
    }


    // --- RESTBase settings start ---

    private void setUpRestBaseCheckboxes() {
        TwoStatePreference manualPreference = (TwoStatePreference) findPreference(getManualKey());
        manualPreference.setOnPreferenceChangeListener(setRestBaseManuallyChangeListener);
        setUseRestBasePreference(manualPreference.isChecked());
    }

    private String getManualKey() {
        return context.getString(R.string.preference_key_use_restbase_manual);
    }

    private void setUseRestBasePreference(boolean manualMode) {
        RbSwitch.INSTANCE.update();
        TwoStatePreference useRestBasePref = getUseRestBasePreference();
        useRestBasePref.setEnabled(manualMode);
        useRestBasePref.setChecked(RbSwitch.INSTANCE.isRestBaseEnabled());
    }

    private TwoStatePreference getUseRestBasePreference() {
        return (TwoStatePreference) findPreference(getUseRestBaseKey());
    }

    private String getUseRestBaseKey() {
        return context.getString(R.string.preference_key_use_restbase);
    }

    // --- RESTBase settings end ---

    // --- MediaWiki settings start ---

    private void setUpMediaWikiSettings() {
        Preference uriPreference = findPreference(getMediaWikiBaseUriKey());
        uriPreference.setOnPreferenceChangeListener(setMediaWikiBaseUriChangeListener);
        TwoStatePreference multiLangPreference
                = (TwoStatePreference) findPreference(getMediaWikiSupportsMultipleLanguages());
        multiLangPreference.setOnPreferenceChangeListener(setMediaWikiMultiLangSupportChangeListener);
    }

    private String getMediaWikiBaseUriKey() {
        return context.getString(R.string.preference_key_mediawiki_base_uri);
    }

    private String getMediaWikiSupportsMultipleLanguages() {
        return context.getString(R.string.preference_key_mediawiki_base_uri_supports_lang_code);
    }

    private void resetMediaWikiSettings() {
        WikipediaApp.getInstance().resetWikiSite();
    }

    // --- MediaWiki settings end ---


    private String getCrashButtonKey() {
        return context.getString(R.string.preferences_developer_crash_key);
    }

    private String getUserOptionButtonKey() {
        return context.getString(R.string.preferences_developer_user_option_key);
    }

    private void setUpCrashButton(Preference button) {
        button.setOnPreferenceClickListener(buildCrashButtonClickListener());
    }

    private void setUpUserOptionButton(Preference button) {
        button.setOnPreferenceClickListener(buildUserOptionButtonClickListener());
    }

    private Preference.OnPreferenceClickListener buildCrashButtonClickListener() {
        return new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                throw new TestException("User tested crash functionality.");
            }
        };
    }

    private Preference.OnPreferenceClickListener buildUserOptionButtonClickListener() {
        return new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                context.startActivity(UserOptionRowActivity.newIntent(context));
                return true;
            }
        };
    }

    private void setUpRemoteLogButton(Preference button) {
        button.setOnPreferenceChangeListener(buildRemoteLogPreferenceChangeListener());
    }

    private Preference.OnPreferenceChangeListener buildRemoteLogPreferenceChangeListener() {
        return new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                L.logRemoteError(new RemoteLogException(newValue.toString()));
                WikipediaApp.getInstance().checkCrashes(getActivity());
                return true;
            }
        };
    }

    private void setUpCookies(@NonNull PreferenceCategory cat) {
        List<String> domains = Prefs.getCookieDomainsAsList();
        for (String domain : domains) {
            String key = Prefs.getCookiesForDomainKey(domain);
            Preference pref = newDataStringPref(key, domain);
            cat.addPreference(pref);
        }
    }

    private EditTextAutoSummarizePreference newDataStringPref(String key, String title) {
        EditTextAutoSummarizePreference pref = new EditTextAutoSummarizePreference(context, null,
                R.attr.editTextAutoSummarizePreferenceStyle);
        pref.setKey(key);
        pref.setTitle(title);
        return pref;
    }

    private static class TestException extends RuntimeException {
        TestException(String message) {
            super(message);
        }
    }
}
