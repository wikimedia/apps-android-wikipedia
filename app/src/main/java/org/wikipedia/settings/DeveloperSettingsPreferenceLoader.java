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
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.ReadingList;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.useroption.ui.UserOptionRowActivity;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.List;

/*package*/ class DeveloperSettingsPreferenceLoader extends BasePreferenceLoader {
    @NonNull private final Context context;
    public static final int MAX_LISTS = 100;

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
        setUpAddArticles(findPreference(R.string.preference_key_add_articles));
        setUpAddLists(findPreference(R.string.preference_key_add_reading_lists));
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

    private void setUpAddArticles(Preference button) {
        button.setOnPreferenceChangeListener(buildAddArticlesPreferenceChangeListener());
    }

    private Preference.OnPreferenceChangeListener buildAddArticlesPreferenceChangeListener() {
        return new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.toString().trim().equals("") || newValue.toString().trim().equals("0")) {
                    return true;
                }
                String title = "Test reading list";
                int listSize = Integer.valueOf(newValue.toString().trim());
                createReadingList(title, listSize);
                return true;
            }
        };
    }

    private void createReadingList(String title, int listSize) {
        long now = System.currentTimeMillis();
        final ReadingList list = ReadingList
                .builder()
                .key(ReadingListDaoProxy.listKey(title))
                .title(title)
                .mtime(now)
                .atime(now)
                .description(null)
                .pages(new ArrayList<ReadingListPage>())
                .build();
        ReadingList.DAO.addList(list);
        for (int i = 0; i < listSize; i++) {
            PageTitle pageTitle = new PageTitle(title.contains("Test") ? "" + (i + 1) : "List" + title.charAt(title.length() - 1) + " Page" + (i + 1), WikipediaApp.getInstance().getWikiSite());
            final ReadingListPage page = ReadingListDaoProxy.page(list, pageTitle);
            ReadingList.DAO.addTitleToList(list, page, false);
        }
    }

    private void setUpAddLists(Preference button) {
        button.setOnPreferenceChangeListener(buildAddListsPreferenceChangeListener());
    }

    private Preference.OnPreferenceChangeListener buildAddListsPreferenceChangeListener() {
        return new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.toString().trim().equals("") || newValue.toString().trim().equals("0")) {
                    return true;
                }
                int numOfLists = Integer.valueOf(newValue.toString().trim());
                numOfLists = numOfLists > MAX_LISTS ? MAX_LISTS : Integer.valueOf(newValue.toString().trim());
                for (int i = 1; i <= numOfLists; i++) {
                    createReadingList("Reading list " + i, 10);
                }
                return true;
            }
        };
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
