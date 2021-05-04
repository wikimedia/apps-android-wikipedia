package org.wikipedia.settings

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.Event
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.notifications.NotificationPollBroadcastReceiver
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListDbHelper
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.setupLeakCanary
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider.getNextArticleWithMissingDescription
import org.wikipedia.talk.TalkPageSeenDatabaseTable.resetAllUnseen
import org.wikipedia.util.StringUtil.fromHtml
import java.util.*

internal class DeveloperSettingsPreferenceLoader(fragment: PreferenceFragmentCompat) : BasePreferenceLoader(fragment) {
    private val setMediaWikiBaseUriChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
        resetMediaWikiSettings()
        true
    }
    private val setMediaWikiMultiLangSupportChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
        resetMediaWikiSettings()
        true
    }

    override fun loadPreferences() {
        loadPreferences(R.xml.developer_preferences)
        setUpMediaWikiSettings()
        findPreference(R.string.preferences_developer_crash_key).onPreferenceClickListener = Preference.OnPreferenceClickListener { throw TestException("User tested crash functionality.") }
        findPreference(R.string.preference_key_add_articles).onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, newValue: Any ->
            if (!isEmptyOrZero(newValue)) {
                createTestReadingList(TEXT_OF_TEST_READING_LIST, 1, newValue.toString().trim().toInt())
            }
            true
        }
        findPreference(R.string.preference_key_add_reading_lists).onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, newValue: Any ->
            if (!isEmptyOrZero(newValue)) {
                createTestReadingList(TEXT_OF_READING_LIST, newValue.toString().trim().toInt(), 10)
            }
            true
        }
        findPreference(R.string.preference_key_delete_reading_lists).onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, newValue: Any ->
            if (!isEmptyOrZero(newValue)) {
                deleteTestReadingList(TEXT_OF_READING_LIST, newValue.toString().trim().toInt())
            }
            true
        }
        findPreference(R.string.preference_key_delete_test_reading_lists).onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, newValue: Any ->
            if (!isEmptyOrZero(newValue)) {
                deleteTestReadingList(TEXT_OF_TEST_READING_LIST, newValue.toString().trim().toInt())
            }
            true
        }
        findPreference(R.string.preference_key_add_malformed_reading_list_page).onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, newValue: Any ->
            val numberOfArticles = if (newValue.toString().isEmpty()) 1 else newValue.toString().trim().toInt()
            val pages: MutableList<ReadingListPage> = ArrayList()
            for (i in 0 until numberOfArticles) {
                val pageTitle = PageTitle("Malformed page $i", WikiSite.forLanguageCode("foo"))
                pages.add(ReadingListPage(pageTitle))
            }
            ReadingListDbHelper.instance().addPagesToList(ReadingListDbHelper.instance().defaultList, pages, true)
            true
        }
        findPreference(R.string.preference_key_missing_description_test).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            getNextArticleWithMissingDescription(WikipediaApp.getInstance().wikiSite, 10)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ summary: PageSummary ->
                        AlertDialog.Builder(activity)
                                .setTitle(fromHtml(summary.displayTitle))
                                .setMessage(fromHtml(summary.extract))
                                .setPositiveButton("Go") { _: DialogInterface, _: Int ->
                                    val title = summary.getPageTitle(WikipediaApp.getInstance().wikiSite)
                                    activity.startActivity(PageActivity.newIntentForNewTab(activity, HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK), title))
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
                    }
                    ) { throwable: Throwable ->
                        AlertDialog.Builder(activity)
                                .setMessage(throwable.message)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                    }
            true
        }
        findPreference(R.string.preference_key_missing_description_test2).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            getNextArticleWithMissingDescription(WikipediaApp.getInstance().wikiSite,
                    WikipediaApp.getInstance().language().appLanguageCodes[1], true, 10)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ (_, second) ->
                        AlertDialog.Builder(activity)
                                .setTitle(fromHtml(second.displayTitle))
                                .setMessage(fromHtml(second.description))
                                .setPositiveButton("Go") { _: DialogInterface, _: Int ->
                                    val title = second.getPageTitle(WikiSite.forLanguageCode(WikipediaApp.getInstance().language().appLanguageCodes[1]))
                                    activity.startActivity(PageActivity.newIntentForNewTab(activity, HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK), title))
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
                    }
                    ) { throwable: Throwable ->
                        AlertDialog.Builder(activity)
                                .setMessage(throwable.message)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                    }
            true
        }
        findPreference(R.string.preference_key_announcement_shown_dialogs).summary = activity.getString(R.string.preferences_developer_announcement_reset_shown_dialogs_summary, Prefs.getAnnouncementShownDialogs().size)
        findPreference(R.string.preference_key_announcement_shown_dialogs).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            Prefs.resetAnnouncementShownDialogs()
            loadPreferences()
            true
        }
        findPreference(R.string.preference_key_suggested_edits_reactivation_test).onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, _: Any? ->
            NotificationPollBroadcastReceiver.stopPollTask(activity)
            NotificationPollBroadcastReceiver.startPollTask(activity)
            true
        }
        findPreference(R.string.preferences_developer_suggested_edits_reactivation_notification_stage_one).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            NotificationPollBroadcastReceiver.showSuggestedEditsLocalNotification(activity, R.string.suggested_edits_reactivation_notification_stage_one)
            true
        }
        findPreference(R.string.preferences_developer_suggested_edits_reactivation_notification_stage_two).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            NotificationPollBroadcastReceiver.showSuggestedEditsLocalNotification(activity, R.string.suggested_edits_reactivation_notification_stage_two)
            true
        }
        findPreference(R.string.preference_developer_clear_all_talk_topics).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            resetAllUnseen()
            true
        }
        findPreference(R.string.preference_key_memory_leak_test).onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, _: Any? ->
            setupLeakCanary()
            true
        }
        findPreference(R.string.preference_key_send_event_platform_test_event).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val event = Event("/analytics/test/1.0.0", "test.instrumentation")
            EventPlatformClient.submit(event)
            true
        }
    }

    private fun setUpMediaWikiSettings() {
        findPreference(R.string.preference_key_mediawiki_base_uri).onPreferenceChangeListener = setMediaWikiBaseUriChangeListener
        findPreference(R.string.preference_key_mediawiki_base_uri_supports_lang_code).onPreferenceChangeListener = setMediaWikiMultiLangSupportChangeListener
    }

    private fun resetMediaWikiSettings() {
        WikipediaApp.getInstance().resetWikiSite()
    }

    private fun createTestReadingList(listName: String, numOfLists: Int, numOfArticles: Int) {
        var index = 0
        ReadingListDbHelper.instance().allListsWithoutContents.asReversed().forEach {
            if (it.title.contains(listName)) {
                val trimmedListTitle = it.title.substring(listName.length).trim()
                index = if (trimmedListTitle.isEmpty()) index else trimmedListTitle.toInt().coerceAtLeast(index)
                return
            }
        }
        for (i in 0 until numOfLists) {
            index += 1
            val list = ReadingListDbHelper.instance().createList("$listName $index", "")
            val pages: MutableList<ReadingListPage> = ArrayList()
            for (j in 0 until numOfArticles) {
                val pageTitle = PageTitle("" + (j + 1), WikipediaApp.getInstance().wikiSite)
                pages.add(ReadingListPage(pageTitle))
            }
            ReadingListDbHelper.instance().addPagesToList(list, pages, true)
        }
    }

    private fun deleteTestReadingList(listName: String, numOfLists: Int) {
        var remainingNumOfLists = numOfLists
        ReadingListDbHelper.instance().allLists.forEach {
            if (it.title.contains(listName) && remainingNumOfLists > 0) {
                ReadingListDbHelper.instance().deleteList(it)
                remainingNumOfLists--
            }
        }
    }

    private fun isEmptyOrZero(newValue: Any): Boolean {
        return newValue.toString().trim().isEmpty() || newValue.toString().trim() == "0"
    }

    private class TestException constructor(message: String?) : RuntimeException(message)

    companion object {
        private const val TEXT_OF_TEST_READING_LIST = "Test reading list"
        private const val TEXT_OF_READING_LIST = "Reading list"
    }
}
