package org.wikipedia.settings

import com.google.gson.reflect.TypeToken
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.analytics.SessionData
import org.wikipedia.analytics.SessionFunnel
import org.wikipedia.analytics.eventplatform.StreamConfig
import org.wikipedia.dataclient.SharedPreferenceCookieManager
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.json.SessionUnmarshaller
import org.wikipedia.json.TabUnmarshaller
import org.wikipedia.page.tabs.Tab
import org.wikipedia.settings.PrefsIoUtil.contains
import org.wikipedia.settings.PrefsIoUtil.getBoolean
import org.wikipedia.settings.PrefsIoUtil.getInt
import org.wikipedia.settings.PrefsIoUtil.getKey
import org.wikipedia.settings.PrefsIoUtil.getLong
import org.wikipedia.settings.PrefsIoUtil.getString
import org.wikipedia.settings.PrefsIoUtil.remove
import org.wikipedia.settings.PrefsIoUtil.setBoolean
import org.wikipedia.settings.PrefsIoUtil.setInt
import org.wikipedia.settings.PrefsIoUtil.setLong
import org.wikipedia.settings.PrefsIoUtil.setString
import org.wikipedia.theme.Theme.Companion.fallback
import org.wikipedia.util.DateUtil.dbDateFormat
import org.wikipedia.util.DateUtil.dbDateParse
import org.wikipedia.util.ReleaseUtil.isDevRelease
import java.util.*

/** Shared preferences utility for convenient POJO access.  */
object Prefs {
    @JvmStatic
    var appChannel: String?
        get() = getString(R.string.preference_key_app_channel, null)
        set(channel) {
            setString(R.string.preference_key_app_channel, channel)
        }
    val appChannelKey: String
        get() = getKey(R.string.preference_key_app_channel)

    // The app install ID uses readingAppInstallID for backwards compatibility with analytics.
    @JvmStatic
    var appInstallId: String?
        get() = getString(R.string.preference_key_reading_app_install_id, null)
        set(id) {
            // The app install ID uses readingAppInstallID for backwards compatibility with analytics.
            setString(R.string.preference_key_reading_app_install_id, id)
        }
    @JvmStatic
    var currentThemeId: Int
        get() = getInt(R.string.preference_key_color_theme, fallback.marshallingId)
        set(theme) {
            setInt(R.string.preference_key_color_theme, theme)
        }
    @JvmStatic
    var previousThemeId: Int
        get() = getInt(R.string.preference_key_previous_color_theme, fallback.marshallingId)
        set(theme) {
            setInt(R.string.preference_key_previous_color_theme, theme)
        }
    @JvmStatic
    var fontFamily: String?
        get() = getString(R.string.preference_key_font_family, "sans-serif")
        set(fontFamily) {
            setString(R.string.preference_key_font_family, fontFamily)
        }

    @JvmStatic
    var cookies: SharedPreferenceCookieManager?
        get() = if (!contains(R.string.preference_key_cookie_map)) {
            null
        } else GsonUnmarshaller.unmarshal(SharedPreferenceCookieManager::class.java,
                getString(R.string.preference_key_cookie_map, null))
        set(cookies) {
            setString(R.string.preference_key_cookie_map, GsonMarshaller.marshal(cookies))
        }

    @JvmStatic
    var crashedBeforeActivityCreated: Boolean
        get() = getBoolean(R.string.preference_key_crashed_before_activity_created, true)
        set(crashed) {
            setBoolean(R.string.preference_key_crashed_before_activity_created, crashed)
        }

    @JvmStatic
    val isCrashReportAutoUploadEnabled: Boolean
        get() = getBoolean(R.string.preference_key_auto_upload_crash_reports, true)

    @JvmStatic
    var isShowDeveloperSettingsEnabled: Boolean
        get() = getBoolean(R.string.preference_key_show_developer_settings, isDevRelease)
        set(enabled) {
            setBoolean(R.string.preference_key_show_developer_settings, enabled)
        }

    @JvmStatic
    var mruLanguageCodeCsv: String?
        get() = getString(R.string.preference_key_language_mru, null)
        set(csv) {
            setString(R.string.preference_key_language_mru, csv)
        }

    @JvmStatic
    var appLanguageCodeCsv: String?
        get() = getString(R.string.preference_key_language_app, null)
        set(csv) {
            setString(R.string.preference_key_language_app, csv)
        }

    @JvmStatic
    var remoteConfigJson: String?
        get() = getString(R.string.preference_key_remote_config, "{}")
        set(json) {
            setString(R.string.preference_key_remote_config, json)
        }

    @JvmStatic
    var tabs: List<Tab>
        get() = if (hasTabs()) TabUnmarshaller.unmarshal(getString(R.string.preference_key_tabs, null)) else emptyList()
        set(tabs) {
            setString(R.string.preference_key_tabs, GsonMarshaller.marshal(tabs))
        }

    @JvmStatic
    fun hasTabs(): Boolean {
        return contains(R.string.preference_key_tabs)
    }

    @JvmStatic
    fun clearTabs() {
        remove(R.string.preference_key_tabs)
    }

    @JvmStatic
    var hiddenCards: Set<String>
        get() {
            val emptySet: Set<String> = LinkedHashSet()
            if (!contains(R.string.preference_key_feed_hidden_cards)) {
                return emptySet
            }
            val cards = GsonUnmarshaller.unmarshal(emptySet.javaClass,
                    getString(R.string.preference_key_feed_hidden_cards, null))
            return cards ?: emptySet
        }
        set(cards) {
            setString(R.string.preference_key_feed_hidden_cards, GsonMarshaller.marshal(cards))
        }

    @JvmStatic
    var sessionData: SessionData
        get() = if (contains(R.string.preference_key_session_data))
            SessionUnmarshaller.unmarshal(getString(R.string.preference_key_session_data, null)) else SessionData()
        set(data) {
            setString(R.string.preference_key_session_data, GsonMarshaller.marshal(data))
        }

    // return the timeout, but don't let it be less than the minimum
    @JvmStatic
    val sessionTimeout: Int
        get() = // return the timeout, but don't let it be less than the minimum
            getInt(R.string.preference_key_session_timeout, SessionFunnel.DEFAULT_SESSION_TIMEOUT).coerceAtLeast(SessionFunnel.MIN_SESSION_TIMEOUT)

    @JvmStatic
    var textSizeMultiplier: Int
        get() = getInt(R.string.preference_key_text_size_multiplier, 0)
        set(multiplier) {
            setInt(R.string.preference_key_text_size_multiplier, multiplier)
        }

    @JvmStatic
    var isEventLoggingEnabled: Boolean
        get() = getBoolean(R.string.preference_key_eventlogging_opt_in, true)
        set(enabled) {
            setBoolean(R.string.preference_key_eventlogging_opt_in, enabled)
        }

    @JvmStatic
    val announcementsCountryOverride: String?
        get() = getString(R.string.preference_key_announcement_country_override, null)

    @JvmStatic
    fun ignoreDateForAnnouncements(): Boolean {
        return getBoolean(R.string.preference_key_announcement_ignore_date, false)
    }

    @JvmStatic
    fun announcementsVersionCode(): Int {
        return getInt(R.string.preference_key_announcement_version_code, 0)
    }

    val retrofitLogLevel: Level
        get() {
            val prefValue = getString(R.string.preference_key_retrofit_log_level, null)
                    ?: return if (isDevRelease) Level.BASIC else Level.NONE
            return when (prefValue) {
                "BASIC" -> Level.BASIC
                "HEADERS" -> Level.HEADERS
                "BODY" -> Level.BODY
                "NONE" -> Level.NONE
                else -> Level.NONE
            }
        }

    @JvmStatic
    val restbaseUriFormat: String
        get() = getString(R.string.preference_key_restbase_uri_format, null).orEmpty().ifBlank { BuildConfig.DEFAULT_RESTBASE_URI_FORMAT }

    @JvmStatic
    val mediaWikiBaseUrl: String?
        get() = getString(R.string.preference_key_mediawiki_base_uri, "")

    @JvmStatic
    val mediaWikiBaseUriSupportsLangCode: Boolean
        get() = getBoolean(R.string.preference_key_mediawiki_base_uri_supports_lang_code, true)

    @JvmStatic
    val eventPlatformIntakeUriOverride: String?
        get() = getString(R.string.preference_key_event_platform_intake_base_uri, null)

    fun getLastRunTime(task: String): Long {
        return getLong(getLastRunTimeKey(task), 0)
    }

    fun setLastRunTime(task: String, time: Long) {
        setLong(getLastRunTimeKey(task), time)
    }

    @JvmStatic
    fun pageLastShown(): Long {
        return getLong(R.string.preference_key_page_last_shown, 0)
    }

    @JvmStatic
    fun pageLastShown(time: Long) {
        setLong(R.string.preference_key_page_last_shown, time)
    }

    @JvmStatic
    var isReadingListTutorialEnabled: Boolean
        get() = getBoolean(R.string.preference_key_reading_list_tutorial_enabled, true)
        set(enabled) {
            setBoolean(R.string.preference_key_reading_list_tutorial_enabled, enabled)
        }

    @JvmStatic
    val isImageDownloadEnabled: Boolean
        get() = getBoolean(R.string.preference_key_show_images, true)

    @JvmStatic
    val isDownloadOnlyOverWiFiEnabled: Boolean
        get() = getBoolean(R.string.preference_key_download_only_over_wifi, false)

    @JvmStatic
    val isDownloadingReadingListArticlesEnabled: Boolean
        get() = getBoolean(R.string.preference_key_download_reading_list_articles, true)

    private fun getLastRunTimeKey(task: String): String {
        return getKey(R.string.preference_key_last_run_time_format, task)
    }

    @JvmStatic
    val isLinkPreviewEnabled: Boolean
        get() = getBoolean(R.string.preference_key_show_link_previews, true)

    val isCollapseTablesEnabled: Boolean
        get() = getBoolean(R.string.preference_key_collapse_tables, true)

    @JvmStatic
    fun getReadingListSortMode(defaultValue: Int): Int {
        return getInt(R.string.preference_key_reading_list_sort_mode, defaultValue)
    }

    @JvmStatic
    fun setReadingListSortMode(sortMode: Int) {
        setInt(R.string.preference_key_reading_list_sort_mode, sortMode)
    }

    @JvmStatic
    var readingListsPageSaveCount: Int
        get() = getInt(R.string.preference_key_save_count_reading_lists, 0)
        set(saveCount) {
            setInt(R.string.preference_key_save_count_reading_lists, saveCount)
        }

    @JvmStatic
    fun getReadingListPageSortMode(defaultValue: Int): Int {
        return getInt(R.string.preference_key_reading_list_page_sort_mode, defaultValue)
    }

    @JvmStatic
    fun setReadingListPageSortMode(sortMode: Int) {
        setInt(R.string.preference_key_reading_list_page_sort_mode, sortMode)
    }

    val isMemoryLeakTestEnabled: Boolean
        get() = getBoolean(R.string.preference_key_memory_leak_test, false)

    @JvmStatic
    var isDescriptionEditTutorialEnabled: Boolean
        get() = getBoolean(R.string.preference_key_description_edit_tutorial_enabled, true)
        set(enabled) {
            setBoolean(R.string.preference_key_description_edit_tutorial_enabled, enabled)
        }

    @JvmStatic
    var lastDescriptionEditTime: Long
        get() = getLong(R.string.preference_key_last_description_edit_time, 0)
        set(time) {
            setLong(R.string.preference_key_last_description_edit_time, time)
        }

    @JvmStatic
    val totalAnonDescriptionsEdited: Int
        get() = getInt(R.string.preference_key_total_anon_descriptions_edited, 0)

    @JvmStatic
    fun incrementTotalAnonDescriptionsEdited() {
        setInt(R.string.preference_key_total_anon_descriptions_edited, totalAnonDescriptionsEdited + 1)
    }

    @JvmStatic
    var isReadingListSyncEnabled: Boolean
        get() = getBoolean(R.string.preference_key_sync_reading_lists, false)
        set(enabled) {
            setBoolean(R.string.preference_key_sync_reading_lists, enabled)
        }

    @JvmStatic
    var isReadingListSyncReminderEnabled: Boolean
        get() = getBoolean(R.string.preference_key_reading_list_sync_reminder_enabled, true)
        set(enabled) {
            setBoolean(R.string.preference_key_reading_list_sync_reminder_enabled, enabled)
        }

    @JvmStatic
    var isReadingListLoginReminderEnabled: Boolean
        get() = getBoolean(R.string.preference_key_reading_list_login_reminder_enabled, true)
        set(enabled) {
            setBoolean(R.string.preference_key_reading_list_login_reminder_enabled, enabled)
        }

    @JvmStatic
    var isReadingListsRemoteDeletePending: Boolean
        get() = getBoolean(R.string.preference_key_reading_lists_remote_delete_pending, false)
        set(pending) {
            setBoolean(R.string.preference_key_reading_lists_remote_delete_pending, pending)
        }

    @JvmStatic
    var isReadingListsRemoteSetupPending: Boolean
        get() = getBoolean(R.string.preference_key_reading_lists_remote_setup_pending, false)
        set(pending) {
            setBoolean(R.string.preference_key_reading_lists_remote_setup_pending, pending)
        }

    @JvmStatic
    var isInitialOnboardingEnabled: Boolean
        get() = getBoolean(R.string.preference_key_initial_onboarding_enabled, true)
        set(enabled) {
            setBoolean(R.string.preference_key_initial_onboarding_enabled, enabled)
        }

    fun askedForPermissionOnce(permission: String): Boolean {
        return getBoolean(R.string.preference_key_permission_asked.toString() + permission, false)
    }

    fun setAskedForPermissionOnce(permission: String) {
        setBoolean(R.string.preference_key_permission_asked.toString() + permission, true)
    }

    var shouldDimDarkModeImages: Boolean
        get() = getBoolean(R.string.preference_key_dim_dark_mode_images, true)
        set(enabled) {
            setBoolean(R.string.preference_key_dim_dark_mode_images, enabled)
        }

    @JvmStatic
    var notificationPollEnabled: Boolean
        get() = getBoolean(R.string.preference_key_notification_poll_enable, true)
        set(enabled) {
            setBoolean(R.string.preference_key_notification_poll_enable, enabled)
        }

    @JvmStatic
    fun notificationPollEnabled(): Boolean {
        return getBoolean(R.string.preference_key_notification_poll_enable, true)
    }

    fun notificationPollReminderEnabled(): Boolean {
        return getBoolean(R.string.preference_key_notification_poll_reminder, true)
    }

    fun setNotificationPollReminderEnabled(enabled: Boolean) {
        setBoolean(R.string.preference_key_notification_poll_reminder, enabled)
    }

    @JvmStatic
    fun notificationWelcomeEnabled(): Boolean {
        return getBoolean(R.string.preference_key_notification_system_enable, true)
    }

    @JvmStatic
    fun notificationMilestoneEnabled(): Boolean {
        return getBoolean(R.string.preference_key_notification_milestone_enable, true)
    }

    @JvmStatic
    fun notificationThanksEnabled(): Boolean {
        return getBoolean(R.string.preference_key_notification_thanks_enable, true)
    }

    @JvmStatic
    fun notificationRevertEnabled(): Boolean {
        return getBoolean(R.string.preference_key_notification_revert_enable, true)
    }

    @JvmStatic
    fun notificationUserTalkEnabled(): Boolean {
        return getBoolean(R.string.preference_key_notification_user_talk_enable, true)
    }

    @JvmStatic
    fun notificationLoginFailEnabled(): Boolean {
        return getBoolean(R.string.preference_key_notification_login_fail_enable, true)
    }

    @JvmStatic
    fun notificationMentionEnabled(): Boolean {
        return getBoolean(R.string.preference_key_notification_mention_enable, true)
    }

    @JvmStatic
    fun showAllNotifications(): Boolean {
        return getBoolean(R.string.preference_key_notification_show_all, false)
    }

    fun preferOfflineContent(): Boolean {
        return getBoolean(R.string.preference_key_prefer_offline_content, false)
    }

    @JvmStatic
    var feedCardsEnabled: List<Boolean>
        get() {
            if (!contains(R.string.preference_key_feed_cards_enabled)) {
                return emptyList()
            }
            return GsonUnmarshaller.unmarshal(object : TypeToken<ArrayList<Boolean>>() {},
                    getString(R.string.preference_key_feed_cards_enabled, null))
        }
        set(enabledList) {
            setString(R.string.preference_key_feed_cards_enabled, GsonMarshaller.marshal(enabledList))
        }
    @JvmStatic
    var feedCardsOrder: List<Int>
        get() {
            if (!contains(R.string.preference_key_feed_cards_order)) {
                return emptyList()
            }
            return GsonUnmarshaller.unmarshal(object : TypeToken<ArrayList<Int>>() {},
                    getString(R.string.preference_key_feed_cards_order, null))
        }
        set(orderList) {
            setString(R.string.preference_key_feed_cards_order, GsonMarshaller.marshal(orderList))
        }
    @JvmStatic
    val feedCardsLangSupported: Map<Int, List<String>>
        get() {
            if (!contains(R.string.preference_key_feed_cards_lang_supported)) {
                return emptyMap()
            }
            return GsonUnmarshaller.unmarshal(object : TypeToken<Map<Int, List<String>>>() {},
                    getString(R.string.preference_key_feed_cards_lang_supported, null))
        }

    @JvmStatic
    fun setFeedCardsLangSupported(langSupportedMap: Map<Int?, List<String?>?>) {
        setString(R.string.preference_key_feed_cards_lang_supported, GsonMarshaller.marshal(langSupportedMap))
    }

    @JvmStatic
    val feedCardsLangDisabled: Map<Int, List<String>>
        get() {
            if (!contains(R.string.preference_key_feed_cards_lang_disabled)) {
                return emptyMap()
            }
            return GsonUnmarshaller.unmarshal(object : TypeToken<Map<Int, List<String>>>() {},
                    getString(R.string.preference_key_feed_cards_lang_disabled, null))
        }

    @JvmStatic
    fun setFeedCardsLangDisabled(langDisabledMap: Map<Int?, List<String?>?>) {
        setString(R.string.preference_key_feed_cards_lang_disabled, GsonMarshaller.marshal(langDisabledMap))
    }

    @JvmStatic
    fun resetFeedCustomizations() {
        remove(R.string.preference_key_feed_hidden_cards)
        remove(R.string.preference_key_feed_cards_enabled)
        remove(R.string.preference_key_feed_cards_order)
        remove(R.string.preference_key_feed_cards_lang_disabled)
    }

    @JvmStatic
    var readingListsLastSyncTime: String?
        get() = getString(R.string.preference_key_reading_lists_last_sync_time, "")
        set(timeStr) {
            setString(R.string.preference_key_reading_lists_last_sync_time, timeStr)
        }
    @JvmStatic
    var readingListsDeletedIds: MutableSet<Long>
        get() {
            val set: MutableSet<Long> = HashSet()
            if (!contains(R.string.preference_key_reading_lists_deleted_ids)) {
                return set
            }
            val tempSet: Set<Long> = GsonUnmarshaller.unmarshal(object : TypeToken<Set<Long>>() {},
                    getString(R.string.preference_key_reading_lists_deleted_ids, null))
            set.addAll(tempSet)
            return set
        }
        set(set) {
            setString(R.string.preference_key_reading_lists_deleted_ids, GsonMarshaller.marshal(set))
        }

    @JvmStatic
    fun addReadingListsDeletedIds(set: MutableSet<Long>) {
        val maxStoredIds = 256
        val currentSet = readingListsDeletedIds
        currentSet.addAll(set)
        readingListsDeletedIds = if (currentSet.size < maxStoredIds) currentSet else set
    }

    @JvmStatic
    var readingListPagesDeletedIds: MutableSet<String>
        get() {
            val set: MutableSet<String> = HashSet()
            if (!contains(R.string.preference_key_reading_lists_deleted_ids)) {
                return set
            }
            val tempSet: Set<String> = GsonUnmarshaller.unmarshal(object : TypeToken<Set<String>>() {},
                    getString(R.string.preference_key_reading_list_pages_deleted_ids, null))
            set.addAll(tempSet)
            return set
        }
        set(set) {
            setString(R.string.preference_key_reading_list_pages_deleted_ids, GsonMarshaller.marshal(set))
        }

    @JvmStatic
    fun addReadingListPagesDeletedIds(set: MutableSet<String>) {
        val maxStoredIds = 256
        val currentSet = readingListPagesDeletedIds
        currentSet.addAll(set)
        readingListPagesDeletedIds = if (currentSet.size < maxStoredIds) currentSet else set
    }

    fun shouldShowReadingListSyncEnablePrompt(): Boolean {
        return getBoolean(R.string.preference_key_show_reading_lists_sync_prompt, true)
    }

    @JvmStatic
    fun shouldShowReadingListSyncEnablePrompt(enabled: Boolean) {
        setBoolean(R.string.preference_key_show_reading_lists_sync_prompt, enabled)
    }

    var isReadingListsFirstTimeSync: Boolean
        get() = getBoolean(R.string.preference_key_reading_lists_first_time_sync, true)
        set(value) {
            setBoolean(R.string.preference_key_reading_lists_first_time_sync, value)
        }
    @JvmStatic
    var editingTextSizeExtra: Int
        get() = getInt(R.string.preference_key_editing_text_size_extra, 0)
        set(extra) {
            setInt(R.string.preference_key_editing_text_size_extra, extra)
        }
    @JvmStatic
    var isMultilingualSearchTutorialEnabled: Boolean
        get() = getBoolean(R.string.preference_key_multilingual_search_tutorial_enabled, true)
        set(enabled) {
            setBoolean(R.string.preference_key_multilingual_search_tutorial_enabled, enabled)
        }

    fun shouldShowRemoveChineseVariantPrompt(enabled: Boolean) {
        setBoolean(R.string.preference_key_show_remove_chinese_variant_prompt, enabled)
    }

    @JvmStatic
    fun shouldShowRemoveChineseVariantPrompt(): Boolean {
        return getBoolean(R.string.preference_key_show_remove_chinese_variant_prompt, true)
    }

    @JvmStatic
    var locallyKnownNotifications: List<Long>
        get() {
            val list: MutableList<Long> = ArrayList()
            if (!contains(R.string.preference_key_locally_known_notifications)) {
                return list
            }
            val tempList: List<Long> = GsonUnmarshaller.unmarshal(object : TypeToken<ArrayList<Long>>() {},
                    getString(R.string.preference_key_locally_known_notifications, null))
            list.addAll(tempList)
            return list
        }
        set(list) {
            setString(R.string.preference_key_locally_known_notifications, GsonMarshaller.marshal(list))
        }
    @JvmStatic
    var remoteNotificationsSeenTime: String?
        get() = getString(R.string.preference_key_remote_notifications_seen_time, "")
        set(seenTime) {
            setString(R.string.preference_key_remote_notifications_seen_time, seenTime)
        }

    fun shouldShowHistoryOfflineArticlesToast(): Boolean {
        return getBoolean(R.string.preference_key_history_offline_articles_toast, true)
    }

    @JvmStatic
    fun shouldShowHistoryOfflineArticlesToast(showToast: Boolean) {
        setBoolean(R.string.preference_key_history_offline_articles_toast, showToast)
    }

    fun wasLoggedOutInBackground(): Boolean {
        return getBoolean(R.string.preference_key_logged_out_in_background, false)
    }

    @JvmStatic
    fun setLoggedOutInBackground(loggedOut: Boolean) {
        setBoolean(R.string.preference_key_logged_out_in_background, loggedOut)
    }

    fun shouldShowDescriptionEditSuccessPrompt(): Boolean {
        return getBoolean(R.string.preference_key_show_description_edit_success_prompt, true)
    }

    @JvmStatic
    fun shouldShowDescriptionEditSuccessPrompt(enabled: Boolean) {
        setBoolean(R.string.preference_key_show_description_edit_success_prompt, enabled)
    }

    @JvmStatic
    var suggestedEditsCountForSurvey: Int
        get() = getInt(R.string.preference_key_suggested_edits_count_for_survey, 0)
        set(count) {
            setInt(R.string.preference_key_suggested_edits_count_for_survey, count)
        }

    @JvmStatic
    fun wasSuggestedEditsSurveyClicked(): Boolean {
        return getBoolean(R.string.preference_key_suggested_edits_survey_clicked, false)
    }

    @JvmStatic
    fun setSuggestedEditsSurveyClicked(surveyClicked: Boolean) {
        setBoolean(R.string.preference_key_suggested_edits_survey_clicked, surveyClicked)
    }

    @JvmStatic
    fun shouldShowSuggestedEditsSurvey(): Boolean {
        return getBoolean(R.string.preference_key_show_suggested_edits_survey, false)
    }

    @JvmStatic
    fun setShouldShowSuggestedEditsSurvey(showSurvey: Boolean) {
        setBoolean(R.string.preference_key_show_suggested_edits_survey, showSurvey)
    }

    @JvmStatic
    fun shouldShowSuggestedEditsTooltip(): Boolean {
        return getBoolean(R.string.preference_key_show_suggested_edits_tooltip, true)
    }

    @JvmStatic
    fun setShouldShowSuggestedEditsTooltip(enabled: Boolean) {
        setBoolean(R.string.preference_key_show_suggested_edits_tooltip, enabled)
    }

    @JvmStatic
    fun hasVisitedArticlePage(): Boolean {
        return getBoolean(R.string.preference_key_visited_article_page, false)
    }

    @JvmStatic
    fun setHasVisitedArticlePage(visited: Boolean) {
        setBoolean(R.string.preference_key_visited_article_page, visited)
    }

    @JvmStatic
    var announcementShownDialogs: MutableSet<String>
        get() {
            val emptySet: MutableSet<String> = LinkedHashSet()
            if (!hasAnnouncementShownDialogs()) {
                return emptySet
            }
            val announcement = GsonUnmarshaller.unmarshal(emptySet.javaClass,
                    getString(R.string.preference_key_announcement_shown_dialogs, null))
            return announcement ?: emptySet
        }
        set(newAnnouncementIds) {
            val announcementIds = announcementShownDialogs
            announcementIds.addAll(newAnnouncementIds)
            setString(R.string.preference_key_announcement_shown_dialogs, GsonMarshaller.marshal(announcementIds))
        }

    private fun hasAnnouncementShownDialogs(): Boolean {
        return contains(R.string.preference_key_announcement_shown_dialogs)
    }

    fun resetAnnouncementShownDialogs() {
        remove(R.string.preference_key_announcement_shown_dialogs)
    }

    var watchlistDisabledLanguages: MutableSet<String>
        get() {
            val emptySet: MutableSet<String> = LinkedHashSet()
            if (!contains(R.string.preference_key_watchlist_disabled_langs)) {
                return emptySet
            }
            val codes = GsonUnmarshaller.unmarshal(emptySet.javaClass,
                    getString(R.string.preference_key_watchlist_disabled_langs, null))
            return codes ?: emptySet
        }
        set(langCodes) {
            val codes = announcementShownDialogs
            codes.addAll(langCodes)
            setString(R.string.preference_key_watchlist_disabled_langs, GsonMarshaller.marshal(langCodes))
        }

    @JvmStatic
    fun shouldMatchSystemTheme(): Boolean {
        return getBoolean(R.string.preference_key_match_system_theme, true)
    }

    fun setMatchSystemTheme(enabled: Boolean) {
        setBoolean(R.string.preference_key_match_system_theme, enabled)
    }

    var suggestedEditsPauseDate: Date?
        get() {
            var date = Date(0)
            if (contains(R.string.preference_key_suggested_edits_pause_date)) {
                date = dbDateParse(getString(R.string.preference_key_suggested_edits_pause_date, "")!!)
            }
            return date
        }
        set(date) {
            setString(R.string.preference_key_suggested_edits_pause_date, dbDateFormat(date))
        }
    var suggestedEditsPauseReverts: Int
        get() = getInt(R.string.preference_key_suggested_edits_pause_reverts, 0)
        set(count) {
            setInt(R.string.preference_key_suggested_edits_pause_reverts, count)
        }

    @JvmStatic
    fun shouldOverrideSuggestedEditCounts(): Boolean {
        return getBoolean(R.string.preference_key_suggested_edits_override_counts, false)
    }

    @JvmStatic
    val overrideSuggestedEditCount: Int
        get() = getInt(R.string.preference_key_suggested_edits_override_edits, 0)

    @JvmStatic
    val overrideSuggestedRevertCount: Int
        get() = getInt(R.string.preference_key_suggested_edits_override_reverts, 0)

    @JvmStatic
    var installReferrerAttempts: Int
        get() = getInt(R.string.preference_key_install_referrer_attempts, 0)
        set(attempts) {
            setInt(R.string.preference_key_install_referrer_attempts, attempts)
        }

    var shouldShowImageTagsOnboarding: Boolean
        get() = getBoolean(R.string.preference_key_image_tags_onboarding_shown, true)
        set(showOnboarding) {
            setBoolean(R.string.preference_key_image_tags_onboarding_shown, showOnboarding)
        }

    var shouldShowImageZoomTooltip: Boolean
        get() = getBoolean(R.string.preference_key_image_zoom_tooltip_shown, true)
        set(show) {
            setBoolean(R.string.preference_key_image_zoom_tooltip_shown, show)
        }

    @JvmStatic
    var isSuggestedEditsReactivationPassStageOne: Boolean
        get() = getBoolean(R.string.preference_key_suggested_edits_reactivation_pass_stage_one, true)
        set(pass) {
            setBoolean(R.string.preference_key_suggested_edits_reactivation_pass_stage_one, pass)
        }

    @JvmStatic
    var temporaryWikitext: String?
        get() = getString(R.string.preference_key_temporary_wikitext_storage, "")
        set(wikitext) {
            setString(R.string.preference_key_temporary_wikitext_storage, wikitext)
        }

    @JvmStatic
    var pushNotificationToken: String?
        get() = getString(R.string.preference_key_push_notification_token, "")
        set(token) {
            setString(R.string.preference_key_push_notification_token, token)
        }

    @JvmStatic
    var pushNotificationTokenOld: String?
        get() = getString(R.string.preference_key_push_notification_token_old, "")
        set(token) {
            setString(R.string.preference_key_push_notification_token_old, token)
        }

    @JvmStatic
    var isPushNotificationTokenSubscribed: Boolean
        get() = getBoolean(R.string.preference_key_push_notification_token_subscribed, false)
        set(subscribed) {
            setBoolean(R.string.preference_key_push_notification_token_subscribed, subscribed)
        }

    val isSuggestedEditsReactivationTestEnabled: Boolean
        get() = getBoolean(R.string.preference_key_suggested_edits_reactivation_test, false)

    @JvmStatic
    var isSuggestedEditsHighestPriorityEnabled: Boolean
        get() = getBoolean(R.string.preference_key_suggested_edits_highest_priority_enabled, false)
        set(enabled) {
            setBoolean(R.string.preference_key_suggested_edits_highest_priority_enabled, enabled)
        }

    @JvmStatic
    fun incrementExploreFeedVisitCount() {
        setInt(R.string.preference_key_explore_feed_visit_count, exploreFeedVisitCount + 1)
    }

    @JvmStatic
    val exploreFeedVisitCount: Int
        get() = getInt(R.string.preference_key_explore_feed_visit_count, 0)

    var selectedLanguagePositionInSearch: Int
        get() = getInt(R.string.preference_key_selected_language_position_in_search, 0)
        set(position) {
            setInt(R.string.preference_key_selected_language_position_in_search, position)
        }

    var shouldShowOneTimeSequentialUserStatsTooltip: Boolean
        get() = getBoolean(R.string.preference_key_show_sequential_user_stats_tooltip, true)
        set(show) {
            setBoolean(R.string.preference_key_show_sequential_user_stats_tooltip, show)
        }

    @JvmStatic
    fun shouldShowSearchTabTooltip(): Boolean {
        return getBoolean(R.string.preference_key_show_search_tab_tooltip, true)
    }

    @JvmStatic
    fun setShowSearchTabTooltip(show: Boolean) {
        setBoolean(R.string.preference_key_show_search_tab_tooltip, show)
    }

    @JvmStatic
    var eventPlatformSessionId: String?
        get() = getString(R.string.preference_key_event_platform_session_id, null)
        set(sessionId) {
            setString(R.string.preference_key_event_platform_session_id, sessionId)
        }

    @JvmStatic
    var streamConfigs: Map<String, StreamConfig>
        get() {
            val streamConfigMapType: TypeToken<HashMap<String, StreamConfig>> = object : TypeToken<HashMap<String, StreamConfig>>() {}
            val streamConfigJson = getString(R.string.preference_key_event_platform_stored_stream_configs, "{}")
            return GsonUnmarshaller.unmarshal(streamConfigMapType, streamConfigJson) as HashMap<String, StreamConfig>
        }
        set(streamConfigs) {
            setString(R.string.preference_key_event_platform_stored_stream_configs, GsonMarshaller.marshal(streamConfigs))
        }

    @JvmStatic
    var localClassName: String?
        get() = getString(R.string.preference_key_crash_report_local_class_name, "")
        set(className) {
            setString(R.string.preference_key_crash_report_local_class_name, className)
        }

    @JvmStatic
    var isWatchlistPageOnboardingTooltipShown: Boolean
        get() = getBoolean(R.string.preference_key_watchlist_page_onboarding_tooltip_shown, false)
        set(enabled) {
            setBoolean(R.string.preference_key_watchlist_page_onboarding_tooltip_shown, enabled)
        }

    @JvmStatic
    var isWatchlistMainOnboardingTooltipShown: Boolean
        get() = getBoolean(R.string.preference_key_watchlist_main_onboarding_tooltip_shown, false)
        set(enabled) {
            setBoolean(R.string.preference_key_watchlist_main_onboarding_tooltip_shown, enabled)
        }
}
