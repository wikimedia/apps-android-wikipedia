package org.wikipedia.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.edits.provider.MissingDescriptionProvider;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.notifications.NotificationPollBroadcastReceiver;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

class DeveloperSettingsPreferenceLoader extends BasePreferenceLoader {
    private static final String TEXT_OF_TEST_READING_LIST = "Test reading list";
    private static final String TEXT_OF_READING_LIST = "Reading list";

    @NonNull private final Context context;

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

    DeveloperSettingsPreferenceLoader(@NonNull PreferenceFragmentCompat fragment) {
        super(fragment);
        this.context = fragment.requireActivity();
    }

    @SuppressLint("CheckResult")
    @SuppressWarnings("checkstyle:methodlength")
    @Override
    public void loadPreferences() {
        loadPreferences(R.xml.developer_preferences);
        setUpMediaWikiSettings();

        findPreference(context.getString(R.string.preferences_developer_crash_key))
                .setOnPreferenceClickListener(preference -> {
                    throw new TestException("User tested crash functionality.");
                });

        findPreference(R.string.preference_key_add_articles)
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    if (newValue.toString().trim().equals("") || newValue.toString().trim().equals("0")) {
                        return true;
                    }

                    int numberOfArticles = Integer.parseInt(newValue.toString().trim());
                    createTestReadingList(TEXT_OF_TEST_READING_LIST, 1, numberOfArticles);

                    return true;
                });

        findPreference(R.string.preference_key_add_reading_lists)
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    if (newValue.toString().trim().equals("") || newValue.toString().trim().equals("0")) {
                        return true;
                    }

                    int numOfLists = Integer.parseInt(newValue.toString().trim());
                    createTestReadingList(TEXT_OF_READING_LIST, numOfLists, 10);

                    return true;
                });

        findPreference(R.string.preference_key_delete_reading_lists)
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    if (newValue.toString().trim().equals("") || newValue.toString().trim().equals("0")) {
                        return true;
                    }
                    int numOfLists = Integer.parseInt(newValue.toString().trim());
                    deleteTestReadingList(TEXT_OF_READING_LIST, numOfLists);
                    return true;
                });
        findPreference(R.string.preference_key_delete_test_reading_lists)
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    if (newValue.toString().trim().equals("") || newValue.toString().trim().equals("0")) {
                        return true;
                    }
                    int numOfLists = Integer.parseInt(newValue.toString().trim());
                    deleteTestReadingList(TEXT_OF_TEST_READING_LIST, numOfLists);
                    return true;
                });

        findPreference(R.string.preference_key_add_malformed_reading_list_page)
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    int numberOfArticles = TextUtils.isEmpty(newValue.toString()) ? 1 :  Integer.parseInt(newValue.toString().trim());
                    List<ReadingListPage> pages = new ArrayList<>();
                    for (int i = 0; i < numberOfArticles; i++) {
                        PageTitle pageTitle = new PageTitle("Malformed page " + i, WikiSite.forLanguageCode("foo"));
                        pages.add(new ReadingListPage(pageTitle));
                    }
                    ReadingListDbHelper.instance().addPagesToList(ReadingListDbHelper.instance().getDefaultList(), pages, true);
                    return true;
                });

        findPreference(context.getString(R.string.preference_key_missing_description_test))
                .setOnPreferenceClickListener(preference -> {
                    MissingDescriptionProvider.INSTANCE.getNextArticleWithMissingDescription(WikipediaApp.getInstance().getWikiSite())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(summary -> new AlertDialog.Builder(getActivity())
                                            .setTitle(StringUtil.fromHtml(summary.getDisplayTitle()))
                                            .setMessage(StringUtil.fromHtml(summary.getExtract()))
                                            .setPositiveButton("Go", (dialog, which) -> {
                                                PageTitle title = new PageTitle(summary.getApiTitle(), WikipediaApp.getInstance().getWikiSite());
                                                getActivity().startActivity(PageActivity.newIntentForNewTab(getActivity(), new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK), title));
                                            })
                                            .setNegativeButton(R.string.cancel, null)
                                            .show(),
                                    throwable -> new AlertDialog.Builder(getActivity())
                                            .setMessage(throwable.getMessage())
                                            .setPositiveButton(R.string.ok, null)
                                            .show());
                    return true;
                });

        findPreference(context.getString(R.string.preference_key_missing_description_test2))
                .setOnPreferenceClickListener(preference -> {
                    MissingDescriptionProvider.INSTANCE.getNextArticleWithMissingDescription(WikipediaApp.getInstance().getWikiSite(),
                            WikipediaApp.getInstance().language().getAppLanguageCodes().get(1), true)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(pair -> new AlertDialog.Builder(getActivity())
                                            .setTitle(StringUtil.fromHtml(pair.getSecond().getDisplayTitle()))
                                            .setMessage(StringUtil.fromHtml(pair.getSecond().getDescription()))
                                            .setPositiveButton("Go", (dialog, which) -> {
                                                PageTitle title = new PageTitle(pair.getSecond().getApiTitle(), WikiSite.forLanguageCode(WikipediaApp.getInstance().language().getAppLanguageCodes().get(1)));
                                                getActivity().startActivity(PageActivity.newIntentForNewTab(getActivity(), new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK), title));
                                            })
                                            .setNegativeButton(R.string.cancel, null)
                                            .show(),
                                    throwable -> new AlertDialog.Builder(getActivity())
                                            .setMessage(throwable.getMessage())
                                            .setPositiveButton(R.string.ok, null)
                                            .show());
                    return true;
                });

        findPreference(context.getString(R.string.preferences_developer_announcement_reset_shown_dialogs_key)).setSummary(context.getString(R.string.preferences_developer_announcement_reset_shown_dialogs_summary, Prefs.getAnnouncementShownDialogs().size()));
        findPreference(context.getString(R.string.preferences_developer_announcement_reset_shown_dialogs_key))
                .setOnPreferenceClickListener(preference -> {
                    Prefs.resetAnnouncementShownDialogs();
                    loadPreferences();
                    return true;
                });

        findPreference(R.string.preference_key_suggested_edits_reactivation_test)
                .setOnPreferenceChangeListener((preference, newValue) -> {
                    NotificationPollBroadcastReceiver.stopPollTask(getActivity());
                    NotificationPollBroadcastReceiver.startPollTask(getActivity());
                    return true;
                });

        findPreference(context.getString(R.string.preferences_developer_suggested_edits_reactivation_notification_stage_one))
                .setOnPreferenceClickListener(preference -> {
                    NotificationPollBroadcastReceiver.showSuggestedEditsLocalNotification(getActivity(), R.string.suggested_edits_reactivation_notification_stage_one);
                    return true;
                });

        findPreference(context.getString(R.string.preferences_developer_suggested_edits_reactivation_notification_stage_two))
                .setOnPreferenceClickListener(preference -> {
                    NotificationPollBroadcastReceiver.showSuggestedEditsLocalNotification(getActivity(), R.string.suggested_edits_reactivation_notification_stage_two);
                    return true;
                });
    }

    private void setUpMediaWikiSettings() {
        Preference uriPreference = findPreference(context.getString(R.string.preference_key_mediawiki_base_uri));
        uriPreference.setOnPreferenceChangeListener(setMediaWikiBaseUriChangeListener);
        TwoStatePreference multiLangPreference
                = (TwoStatePreference) findPreference(context.getString(R.string.preference_key_mediawiki_base_uri_supports_lang_code));
        multiLangPreference.setOnPreferenceChangeListener(setMediaWikiMultiLangSupportChangeListener);
    }

    private void resetMediaWikiSettings() {
        WikipediaApp.getInstance().resetWikiSite();
    }

    private void createTestReadingList(String listName, int numOfLists, int numOfArticles) {
        int index = 0;

        List<ReadingList> lists = ReadingListDbHelper.instance().getAllListsWithoutContents();
        for (int i = lists.size() - 1; i >= 0; i--) {
            ReadingList lastReadingList = lists.get(i);
            if (lastReadingList.title().contains(listName)) {
                String trimmedListTitle = lastReadingList.title().substring(listName.length()).trim();
                index = trimmedListTitle.isEmpty() ? index : Math.max(Integer.parseInt(trimmedListTitle), index);
                break;
            }
        }

        for (int i = 0; i < numOfLists; i++) {
            index += 1;
            ReadingList list = ReadingListDbHelper.instance().createList(listName + " " + index, "");
            List<ReadingListPage> pages = new ArrayList<>();
            for (int j = 0; j < numOfArticles; j++) {
                PageTitle pageTitle = new PageTitle("" + (j + 1), WikipediaApp.getInstance().getWikiSite());
                pages.add(new ReadingListPage(pageTitle));
            }
            ReadingListDbHelper.instance().addPagesToList(list, pages, true);
        }
    }

    private void deleteTestReadingList(String listName, int numOfLists) {
        List<ReadingList> lists = ReadingListDbHelper.instance().getAllLists();
        for (ReadingList list : lists) {
            if (list.title().contains(listName) && numOfLists > 0) {
                ReadingListDbHelper.instance().deleteList(list);
                numOfLists--;
            }
        }
    }

    private static class TestException extends RuntimeException {
        TestException(String message) {
            super(message);
        }
    }
}
