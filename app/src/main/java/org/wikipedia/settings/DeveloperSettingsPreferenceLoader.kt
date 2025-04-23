package org.wikipedia.settings

import android.content.DialogInterface
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.games.onthisday.OnThisDayGameNotificationManager
import org.wikipedia.games.onthisday.OnThisDayGameNotificationState
import org.wikipedia.history.HistoryEntry
import org.wikipedia.notifications.NotificationPollBroadcastReceiver
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.setupLeakCanary
import org.wikipedia.suggestededits.provider.EditingSuggestionsProvider
import org.wikipedia.util.CategoryTestUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil.fromHtml

internal class DeveloperSettingsPreferenceLoader(fragment: PreferenceFragmentCompat) : BasePreferenceLoader(fragment) {
    private val setMediaWikiBaseUriChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
        resetMediaWikiSettings()
        true
    }
    private val setMediaWikiMultiLangSupportChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
        resetMediaWikiSettings()
        true
    }

    fun filterPreferences(query: String? = null) {
        query?.let {
            for (i in 0 until fragment.preferenceScreen.preferenceCount) {
                filterPreferenceGroupItems(fragment.preferenceScreen.getPreference(i), query)
            }
        } ?: run {
            clearPreferences()
            loadPreferences()
        }
    }

    private fun filterPreferenceGroupItems(preference: Preference, query: String): Boolean {
        if (preference is PreferenceGroup) {
            var visibleChildCount = 0
            for (i in 0 until preference.preferenceCount) {
                if (filterPreferenceGroupItems(preference.getPreference(i), query)) {
                    visibleChildCount++
                }
            }

            // Hide the group if no children are visible
            preference.isVisible = visibleChildCount > 0
            return preference.isVisible
        } else {
            val isPrefVisible = preference.title?.toString()?.contains(query, ignoreCase = true) == true ||
                    preference.summary?.toString()?.contains(query, ignoreCase = true) == true
            preference.isVisible = isPrefVisible
            return isPrefVisible
        }
    }

    override fun loadPreferences() {
        loadPreferences(R.xml.developer_preferences)
        setUpMediaWikiSettings()
        findPreference(R.string.preferences_developer_crash_key).onPreferenceClickListener = Preference.OnPreferenceClickListener { throw TestException("User tested crash functionality.") }
        findPreference(R.string.preference_key_add_articles).onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, newValue: Any ->
            val intValue = newValue.toIntOrDefault()
            if (intValue != 0) {
                createTestReadingList(TEXT_OF_TEST_READING_LIST, 1, intValue)
            }
            true
        }
        findPreference(R.string.preference_key_add_reading_lists).onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, newValue: Any ->
            val intValue = newValue.toIntOrDefault()
            if (intValue != 0) {
                createTestReadingList(TEXT_OF_READING_LIST, intValue, 10)
            }
            true
        }
        findPreference(R.string.preference_key_delete_reading_lists).onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, newValue: Any ->
            val intValue = newValue.toIntOrDefault()
            if (intValue != 0) {
                deleteTestReadingList(TEXT_OF_READING_LIST, intValue)
            }
            true
        }
        findPreference(R.string.preference_key_delete_test_reading_lists).onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, newValue: Any ->
            val intValue = newValue.toIntOrDefault()
            if (intValue != 0) {
                deleteTestReadingList(TEXT_OF_TEST_READING_LIST, intValue)
            }
            true
        }
        findPreference(R.string.preference_key_add_malformed_reading_list_page).onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, newValue: Any ->
            val numberOfArticles = newValue.toIntOrDefault(1)
            val pages = (0 until numberOfArticles).map {
                ReadingListPage(PageTitle("Malformed page $it", WikiSite.forLanguageCode("foo")))
            }
            AppDatabase.instance.readingListPageDao().addPagesToList(AppDatabase.instance.readingListDao().getDefaultList(), pages, true)
            true
        }
        findPreference(R.string.preference_key_missing_description_test).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            fragment.lifecycleScope.launch(CoroutineExceptionHandler { _, caught ->
                MaterialAlertDialogBuilder(activity)
                    .setMessage(caught.message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }) {
                val summary = EditingSuggestionsProvider.getNextArticleWithMissingDescription(WikipediaApp.instance.wikiSite)
                MaterialAlertDialogBuilder(fragment.requireActivity())
                        .setTitle(fromHtml(summary.displayTitle))
                        .setMessage(fromHtml(summary.extract))
                        .setPositiveButton("Go") { _: DialogInterface, _: Int ->
                            val title = summary.getPageTitle(WikipediaApp.instance.wikiSite)
                            fragment.requireActivity().startActivity(PageActivity.newIntentForNewTab(fragment.requireActivity(), HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK), title))
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
            }
            true
        }
        findPreference(R.string.preference_key_missing_description_test2).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            fragment.lifecycleScope.launch(CoroutineExceptionHandler { _, caught ->
                MaterialAlertDialogBuilder(activity)
                    .setMessage(caught.message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }) {
                val summary = EditingSuggestionsProvider.getNextArticleWithMissingDescription(WikipediaApp.instance.wikiSite,
                    WikipediaApp.instance.languageState.appLanguageCodes[1])
                MaterialAlertDialogBuilder(fragment.requireActivity())
                        .setTitle(fromHtml(summary.second.displayTitle))
                        .setMessage(fromHtml(summary.second.extract))
                        .setPositiveButton("Go") { _: DialogInterface, _: Int ->
                            val title = summary.second.getPageTitle(WikipediaApp.instance.wikiSite)
                            fragment.requireActivity().startActivity(PageActivity.newIntentForNewTab(fragment.requireActivity(), HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK), title))
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
            }
            true
        }
        findPreference(R.string.preference_key_announcement_shown_dialogs).summary = activity.getString(R.string.preferences_developer_announcement_reset_shown_dialogs_summary, Prefs.announcementShownDialogs.size)
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
            CoroutineScope(Dispatchers.Main).launch {
                AppDatabase.instance.talkPageSeenDao().deleteAll()
                Toast.makeText(activity, "Reset complete.", Toast.LENGTH_SHORT).show()
            }
            true
        }
        findPreference(R.string.preference_developer_clear_last_location_and_zoom_level).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            Prefs.placesLastLocationAndZoomLevel = null
            Toast.makeText(activity, "Reset complete.", Toast.LENGTH_SHORT).show()
            true
        }
        findPreference(R.string.preference_key_memory_leak_test).onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, _: Any? ->
            setupLeakCanary()
            true
        }
        findPreference(R.string.preference_key_feed_yir_onboarding_card_enabled).onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference, isEnabled: Any? ->
            if (isEnabled is Boolean && isEnabled) {
                Prefs.hiddenCards = emptySet()
                Toast.makeText(activity, "Please relaunch the app.", Toast.LENGTH_SHORT).show()
            }
            true
        }
        findPreference(R.string.preference_key_otd_game_state).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            Prefs.otdGameState = ""
            Toast.makeText(activity, "Game reset.", Toast.LENGTH_SHORT).show()
            true
        }
        findPreference(R.string.preferences_developer_otd_show_notification).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            OnThisDayGameNotificationManager.showNotification(activity)
            true
        }
        findPreference(R.string.preference_key_otd_notification_state).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            Prefs.otdNotificationState = OnThisDayGameNotificationState.NO_INTERACTED
            OnThisDayGameNotificationManager.cancelDailyGameNotification(activity)
            FeedbackUtil.showMessage(activity, "Notification state reset.")
            true
        }
        findPreference(R.string.preference_key_year_in_review_add_categories).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            CategoryTestUtil.addTestData(activity)
            true
        }
        findPreference(R.string.preference_key_year_in_review_delete_all_categories).onPreferenceClickListener = Preference.OnPreferenceClickListener {
            CategoryTestUtil.deleteAllCategories(activity)
            true
        }
    }

    private fun setUpMediaWikiSettings() {
        findPreference(R.string.preference_key_mediawiki_base_uri).onPreferenceChangeListener = setMediaWikiBaseUriChangeListener
        findPreference(R.string.preference_key_mediawiki_base_uri_supports_lang_code).onPreferenceChangeListener = setMediaWikiMultiLangSupportChangeListener
    }

    private fun resetMediaWikiSettings() {
        WikipediaApp.instance.resetWikiSite()
    }

    private fun createTestReadingList(listName: String, numOfLists: Int, numOfArticles: Int) {
        var index = 0
        AppDatabase.instance.readingListDao().getListsWithoutContents().asReversed().forEach {
            if (it.title.contains(listName)) {
                val trimmedListTitle = it.title.substring(listName.length).trim()
                index = trimmedListTitle.toIntOrNull()?.coerceAtLeast(index) ?: index
                return
            }
        }
        for (i in 0 until numOfLists) {
            index += 1
            val list = AppDatabase.instance.readingListDao().createList("$listName $index", "")
            val pages = (0 until numOfArticles).map {
                ReadingListPage(PageTitle("${it + 1}", WikipediaApp.instance.wikiSite))
            }
            AppDatabase.instance.readingListPageDao().addPagesToList(list, pages, true)
        }
    }

    private fun deleteTestReadingList(listName: String, numOfLists: Int) {
        var remainingNumOfLists = numOfLists
        AppDatabase.instance.readingListDao().getAllLists().forEach {
            if (it.title.contains(listName) && remainingNumOfLists > 0) {
                AppDatabase.instance.readingListDao().deleteList(it)
                remainingNumOfLists--
            }
        }
    }

    private fun Any.toIntOrDefault(defaultValue: Int = 0): Int {
        return toString().trim().toIntOrNull() ?: defaultValue
    }

    private class TestException(message: String?) : RuntimeException(message)

    companion object {
        private const val TEXT_OF_TEST_READING_LIST = "Test reading list"
        private const val TEXT_OF_READING_LIST = "Reading list"
    }
}
