package org.wikipedia.settings

import com.google.gson.reflect.TypeToken
import okhttp3.logging.HttpLoggingInterceptor
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
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.theme.Theme.Companion.fallback
import org.wikipedia.util.DateUtil.dbDateFormat
import org.wikipedia.util.DateUtil.dbDateParse
import org.wikipedia.util.ReleaseUtil.isDevRelease
import java.util.*

/** Shared preferences utility for convenient POJO access.  */
object Prefs {
    var appChannel: String?
        get() = PrefsIoUtil.getString(R.string.preference_key_app_channel, null)
        set(channel) {
            PrefsIoUtil.setString(R.string.preference_key_app_channel, channel)
        }

    val appChannelKey: String
        get() = PrefsIoUtil.getKey(R.string.preference_key_app_channel)

    // The app install ID uses readingAppInstallID for backwards compatibility with analytics.
    @JvmStatic
    var appInstallId: String?
        get() = PrefsIoUtil.getString(R.string.preference_key_reading_app_install_id, null)
        set(id) {
            // The app install ID uses readingAppInstallID for backwards compatibility with analytics.
            PrefsIoUtil.setString(R.string.preference_key_reading_app_install_id, id)
        }

    @JvmStatic
    var currentThemeId: Int
        get() = PrefsIoUtil.getInt(R.string.preference_key_color_theme, fallback.marshallingId)
        set(theme) {
            PrefsIoUtil.setInt(R.string.preference_key_color_theme, theme)
        }

    var previousThemeId: Int
        get() = PrefsIoUtil.getInt(
            R.string.preference_key_previous_color_theme,
            fallback.marshallingId
        )
        set(theme) {
            PrefsIoUtil.setInt(R.string.preference_key_previous_color_theme, theme)
        }

    @JvmStatic
    var fontFamily: String
        get() = PrefsIoUtil.getString(R.string.preference_key_font_family, "sans-serif")!!
        set(fontFamily) {
            PrefsIoUtil.setString(R.string.preference_key_font_family, fontFamily)
        }

    @JvmStatic
    var cookies: SharedPreferenceCookieManager?
        get() = if (!PrefsIoUtil.contains(R.string.preference_key_cookie_map)) {
            null
        } else GsonUnmarshaller.unmarshal(
            SharedPreferenceCookieManager::class.java,
            PrefsIoUtil.getString(R.string.preference_key_cookie_map, null)
        )
        set(cookies) {
            PrefsIoUtil.setString(
                R.string.preference_key_cookie_map,
                GsonMarshaller.marshal(cookies)
            )
        }

    var isCrashedBeforeActivityCreated: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_crashed_before_activity_created,
            true
        )
        set(crashed) {
            PrefsIoUtil.setBoolean(R.string.preference_key_crashed_before_activity_created, crashed)
        }

    var isShowDeveloperSettingsEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_show_developer_settings,
            isDevRelease
        )
        set(enabled) {
            PrefsIoUtil.setBoolean(R.string.preference_key_show_developer_settings, enabled)
        }

    @JvmStatic
    var mruLanguageCodeCsv: String?
        get() = PrefsIoUtil.getString(R.string.preference_key_language_mru, null)
        set(csv) {
            PrefsIoUtil.setString(R.string.preference_key_language_mru, csv)
        }

    @JvmStatic
    var appLanguageCodeCsv: String?
        get() = PrefsIoUtil.getString(R.string.preference_key_language_app, null)
        set(csv) {
            PrefsIoUtil.setString(R.string.preference_key_language_app, csv)
        }

    var remoteConfigJson: String
        get() = PrefsIoUtil.getString(R.string.preference_key_remote_config, "{}")!!
        set(json) {
            PrefsIoUtil.setString(R.string.preference_key_remote_config, json)
        }

    @JvmStatic
    var tabs: List<Tab>
        get() = if (hasTabs()) TabUnmarshaller.unmarshal(
            PrefsIoUtil.getString(
                R.string.preference_key_tabs,
                null
            )
        ) else emptyList()
        set(tabs) {
            PrefsIoUtil.setString(R.string.preference_key_tabs, GsonMarshaller.marshal(tabs))
        }

    @JvmStatic
    fun hasTabs(): Boolean {
        return PrefsIoUtil.contains(R.string.preference_key_tabs)
    }

    @JvmStatic
    fun clearTabs() {
        PrefsIoUtil.remove(R.string.preference_key_tabs)
    }

    var hiddenCards: Set<String>
        get() {
            val emptySet: Set<String> = LinkedHashSet()
            if (!hasHiddenCards()) {
                return emptySet
            }
            val cards = GsonUnmarshaller.unmarshal(
                emptySet.javaClass,
                PrefsIoUtil.getString(R.string.preference_key_feed_hidden_cards, null)
            )
            return cards ?: emptySet
        }
        set(cards) {
            PrefsIoUtil.setString(
                R.string.preference_key_feed_hidden_cards,
                GsonMarshaller.marshal(cards)
            )
        }

    fun hasHiddenCards(): Boolean {
        return PrefsIoUtil.contains(R.string.preference_key_feed_hidden_cards)
    }

    var sessionData: SessionData
        get() = if (hasSessionData()) SessionUnmarshaller.unmarshal(
            PrefsIoUtil.getString(
                R.string.preference_key_session_data,
                null
            )
        ) else SessionData()
        set(data) {
            PrefsIoUtil.setString(
                R.string.preference_key_session_data,
                GsonMarshaller.marshal(data)
            )
        }

    fun hasSessionData(): Boolean {
        return PrefsIoUtil.contains(R.string.preference_key_session_data)
    }

    // return the timeout, but don't let it be less than the minimum
    val sessionTimeout: Int
        get() = PrefsIoUtil.getInt(R.string.preference_key_session_timeout, SessionFunnel.DEFAULT_SESSION_TIMEOUT)
            .coerceAtLeast(SessionFunnel.MIN_SESSION_TIMEOUT)

    @JvmStatic
    var textSizeMultiplier: Int
        get() = PrefsIoUtil.getInt(R.string.preference_key_text_size_multiplier, 0)
        set(multiplier) {
            PrefsIoUtil.setInt(R.string.preference_key_text_size_multiplier, multiplier)
        }

    var isEventLoggingEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_eventlogging_opt_in, true)
        set(enabled) {
            PrefsIoUtil.setBoolean(R.string.preference_key_eventlogging_opt_in, enabled)
        }

    val announcementsCountryOverride: String?
        get() = PrefsIoUtil.getString(R.string.preference_key_announcement_country_override, null)

    fun ignoreDateForAnnouncements(): Boolean {
        return PrefsIoUtil.getBoolean(R.string.preference_key_announcement_ignore_date, false)
    }

    fun announcementsVersionCode(): Int {
        return PrefsIoUtil.getInt(R.string.preference_key_announcement_version_code, 0)
    }

    val retrofitLogLevel: HttpLoggingInterceptor.Level
        get() {
            val prefValue = PrefsIoUtil.getString(R.string.preference_key_retrofit_log_level, null)
                ?: return if (isDevRelease) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            return when (prefValue) {
                "BASIC" -> HttpLoggingInterceptor.Level.BASIC
                "HEADERS" -> HttpLoggingInterceptor.Level.HEADERS
                "BODY" -> HttpLoggingInterceptor.Level.BODY
                "NONE" -> HttpLoggingInterceptor.Level.NONE
                else -> HttpLoggingInterceptor.Level.NONE
            }
        }

    val restbaseUriFormat: String
        get() = PrefsIoUtil.getString(R.string.preference_key_restbase_uri_format, null).orEmpty().ifEmpty {
            BuildConfig.DEFAULT_RESTBASE_URI_FORMAT
        }

    @JvmStatic
    val mediaWikiBaseUrl: String
        get() = PrefsIoUtil.getString(R.string.preference_key_mediawiki_base_uri, "")!!

    @JvmStatic
    val mediaWikiBaseUriSupportsLangCode: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_mediawiki_base_uri_supports_lang_code,
            true
        )

    val eventPlatformIntakeUriOverride: String
        get() = PrefsIoUtil.getString(R.string.preference_key_event_platform_intake_base_uri, "")!!

    fun getLastRunTime(task: String): Long {
        return PrefsIoUtil.getLong(getLastRunTimeKey(task), 0)
    }

    fun setLastRunTime(task: String, time: Long) {
        PrefsIoUtil.setLong(getLastRunTimeKey(task), time)
    }

    var pageLastShown: Long
        get() = PrefsIoUtil.getLong(R.string.preference_key_page_last_shown, 0)
        set(time) {
            PrefsIoUtil.setLong(R.string.preference_key_page_last_shown, time)
        }

    val isImageDownloadEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_images, true)

    val isDownloadOnlyOverWiFiEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_download_only_over_wifi, false)

    val isDownloadingReadingListArticlesEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_download_reading_list_articles, true)

    private fun getLastRunTimeKey(task: String): String {
        return PrefsIoUtil.getKey(R.string.preference_key_last_run_time_format, task)
    }

    val isLinkPreviewEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_link_previews, true)

    val isCollapseTablesEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_collapse_tables, true)

    var readingListSortMode: Int
        get() = PrefsIoUtil.getInt(
            R.string.preference_key_reading_list_sort_mode,
            ReadingList.SORT_BY_NAME_ASC
        )
        set(sortMode) {
            PrefsIoUtil.setInt(R.string.preference_key_reading_list_sort_mode, sortMode)
        }

    var readingListsPageSaveCount: Int
        get() = PrefsIoUtil.getInt(R.string.preference_key_save_count_reading_lists, 0)
        set(saveCount) {
            PrefsIoUtil.setInt(R.string.preference_key_save_count_reading_lists, saveCount)
        }

    var readingListPageSortMode: Int
        get() = PrefsIoUtil.getInt(
            R.string.preference_key_reading_list_page_sort_mode,
            ReadingList.SORT_BY_NAME_ASC
        )
        set(sortMode) {
            PrefsIoUtil.setInt(R.string.preference_key_reading_list_page_sort_mode, sortMode)
        }

    val isMemoryLeakTestEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_memory_leak_test, false)

    var isDescriptionEditTutorialEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_description_edit_tutorial_enabled,
            true
        )
        set(enabled) {
            PrefsIoUtil.setBoolean(
                R.string.preference_key_description_edit_tutorial_enabled,
                enabled
            )
        }

    var lastDescriptionEditTime: Long
        get() = PrefsIoUtil.getLong(R.string.preference_key_last_description_edit_time, 0)
        set(time) {
            PrefsIoUtil.setLong(R.string.preference_key_last_description_edit_time, time)
        }

    val totalAnonDescriptionsEdited: Int
        get() = PrefsIoUtil.getInt(R.string.preference_key_total_anon_descriptions_edited, 0)

    fun incrementTotalAnonDescriptionsEdited() {
        PrefsIoUtil.setInt(R.string.preference_key_total_anon_descriptions_edited, totalAnonDescriptionsEdited + 1)
    }

    var isReadingListSyncEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_sync_reading_lists, false)
        set(enabled) {
            PrefsIoUtil.setBoolean(R.string.preference_key_sync_reading_lists, enabled)
        }

    var isReadingListSyncReminderEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_reading_list_sync_reminder_enabled,
            true
        )
        set(enabled) {
            PrefsIoUtil.setBoolean(
                R.string.preference_key_reading_list_sync_reminder_enabled,
                enabled
            )
        }

    var isReadingListLoginReminderEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_reading_list_login_reminder_enabled,
            true
        )
        set(enabled) {
            PrefsIoUtil.setBoolean(
                R.string.preference_key_reading_list_login_reminder_enabled,
                enabled
            )
        }

    var isReadingListsRemoteDeletePending: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_reading_lists_remote_delete_pending,
            false
        )
        set(pending) {
            PrefsIoUtil.setBoolean(
                R.string.preference_key_reading_lists_remote_delete_pending,
                pending
            )
        }

    var isReadingListsRemoteSetupPending: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_reading_lists_remote_setup_pending,
            false
        )
        set(pending) {
            PrefsIoUtil.setBoolean(
                R.string.preference_key_reading_lists_remote_setup_pending,
                pending
            )
        }

    var isInitialOnboardingEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_initial_onboarding_enabled, true)
        set(enabled) {
            PrefsIoUtil.setBoolean(R.string.preference_key_initial_onboarding_enabled, enabled)
        }

    fun askedForPermissionOnce(permission: String): Boolean {
        return PrefsIoUtil.getBoolean(
            R.string.preference_key_permission_asked.toString() + permission,
            false
        )
    }

    fun setAskedForPermissionOnce(permission: String) {
        PrefsIoUtil.setBoolean(
            R.string.preference_key_permission_asked.toString() + permission,
            true
        )
    }

    var shouldDimDarkModeImages: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_dim_dark_mode_images, true)
        set(enabled) {
            PrefsIoUtil.setBoolean(R.string.preference_key_dim_dark_mode_images, enabled)
        }

    var isNotificationPollEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_notification_poll_enable, true)
        set(enabled) {
            PrefsIoUtil.setBoolean(R.string.preference_key_notification_poll_enable, enabled)
        }

    var isNotificationPollReminderEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_notification_poll_reminder, true)
        set(enabled) {
            PrefsIoUtil.setBoolean(R.string.preference_key_notification_poll_reminder, enabled)
        }

    fun notificationWelcomeEnabled(): Boolean {
        return PrefsIoUtil.getBoolean(R.string.preference_key_notification_system_enable, true)
    }

    fun notificationMilestoneEnabled(): Boolean {
        return PrefsIoUtil.getBoolean(R.string.preference_key_notification_milestone_enable, true)
    }

    fun notificationThanksEnabled(): Boolean {
        return PrefsIoUtil.getBoolean(R.string.preference_key_notification_thanks_enable, true)
    }

    fun notificationRevertEnabled(): Boolean {
        return PrefsIoUtil.getBoolean(R.string.preference_key_notification_revert_enable, true)
    }

    fun notificationUserTalkEnabled(): Boolean {
        return PrefsIoUtil.getBoolean(R.string.preference_key_notification_user_talk_enable, true)
    }

    fun notificationLoginFailEnabled(): Boolean {
        return PrefsIoUtil.getBoolean(R.string.preference_key_notification_login_fail_enable, true)
    }

    fun notificationMentionEnabled(): Boolean {
        return PrefsIoUtil.getBoolean(R.string.preference_key_notification_mention_enable, true)
    }

    fun showAllNotifications(): Boolean {
        return PrefsIoUtil.getBoolean(R.string.preference_key_notification_show_all, false)
    }

    fun preferOfflineContent(): Boolean {
        return PrefsIoUtil.getBoolean(R.string.preference_key_prefer_offline_content, false)
    }

    var feedCardsEnabled: List<Boolean>
        get() {
            if (!PrefsIoUtil.contains(R.string.preference_key_feed_cards_enabled)) {
                return emptyList()
            }
            val enabledList = GsonUnmarshaller.unmarshal(
                object : TypeToken<ArrayList<Boolean>>() {},
                PrefsIoUtil.getString(R.string.preference_key_feed_cards_enabled, null)
            )
            return enabledList ?: emptyList()
        }

        set(enabledList) {
            PrefsIoUtil.setString(
                R.string.preference_key_feed_cards_enabled,
                GsonMarshaller.marshal(enabledList)
            )
        }

    var feedCardsOrder: List<Int>
        get() {
            if (!PrefsIoUtil.contains(R.string.preference_key_feed_cards_order)) {
                return emptyList()
            }
            val orderList = GsonUnmarshaller.unmarshal(
                object : TypeToken<ArrayList<Int>>() {},
                PrefsIoUtil.getString(R.string.preference_key_feed_cards_order, null)
            )
            return orderList ?: emptyList()
        }
        set(orderList) {
            PrefsIoUtil.setString(
                R.string.preference_key_feed_cards_order,
                GsonMarshaller.marshal(orderList)
            )
        }

    var feedCardsLangSupported: Map<Int, List<String>>
        get() {
            if (!PrefsIoUtil.contains(R.string.preference_key_feed_cards_lang_supported)) {
                return emptyMap()
            }
            val map = GsonUnmarshaller.unmarshal(
                object : TypeToken<Map<Int, List<String>>>() {},
                PrefsIoUtil.getString(R.string.preference_key_feed_cards_lang_supported, null)
            )
            return map ?: emptyMap()
        }
        set(value) {
            PrefsIoUtil.setString(
                R.string.preference_key_feed_cards_lang_supported,
                GsonMarshaller.marshal(value)
            )
        }

    var feedCardsLangDisabled: Map<Int, List<String>>
        get() {
            if (!PrefsIoUtil.contains(R.string.preference_key_feed_cards_lang_disabled)) {
                return emptyMap()
            }
            val map = GsonUnmarshaller.unmarshal(
                object : TypeToken<Map<Int, List<String>>>() {},
                PrefsIoUtil.getString(R.string.preference_key_feed_cards_lang_disabled, null)
            )
            return map ?: emptyMap()
        }
        set(value) {
            PrefsIoUtil.setString(
                R.string.preference_key_feed_cards_lang_disabled,
                GsonMarshaller.marshal(value)
            )
        }

    fun resetFeedCustomizations() {
        PrefsIoUtil.remove(R.string.preference_key_feed_hidden_cards)
        PrefsIoUtil.remove(R.string.preference_key_feed_cards_enabled)
        PrefsIoUtil.remove(R.string.preference_key_feed_cards_order)
        PrefsIoUtil.remove(R.string.preference_key_feed_cards_lang_disabled)
    }

    var readingListsLastSyncTime: String
        get() = PrefsIoUtil.getString(R.string.preference_key_reading_lists_last_sync_time, "")!!
        set(timeStr) {
            PrefsIoUtil.setString(R.string.preference_key_reading_lists_last_sync_time, timeStr)
        }

    var readingListsDeletedIds: MutableSet<Long>
        get() {
            val set: MutableSet<Long> = HashSet()
            if (!PrefsIoUtil.contains(R.string.preference_key_reading_lists_deleted_ids)) {
                return set
            }
            val tempSet = GsonUnmarshaller.unmarshal(
                object : TypeToken<Set<Long>>() {},
                PrefsIoUtil.getString(R.string.preference_key_reading_lists_deleted_ids, null)
            )
            if (tempSet != null) {
                set.addAll(tempSet)
            }
            return set
        }
        set(set) {
            PrefsIoUtil.setString(
                R.string.preference_key_reading_lists_deleted_ids,
                GsonMarshaller.marshal(set)
            )
        }

    fun addReadingListsDeletedIds(set: Set<Long>) {
        val maxStoredIds = 256
        val currentSet = readingListsDeletedIds
        currentSet.addAll(set)
        readingListsDeletedIds = if (currentSet.size < maxStoredIds) currentSet else set.toMutableSet()
    }

    var readingListPagesDeletedIds: MutableSet<String>
        get() {
            val set: MutableSet<String> = HashSet()
            if (!PrefsIoUtil.contains(R.string.preference_key_reading_lists_deleted_ids)) {
                return set
            }
            val tempSet = GsonUnmarshaller.unmarshal(
                object : TypeToken<Set<String>>() {},
                PrefsIoUtil.getString(R.string.preference_key_reading_list_pages_deleted_ids, null)
            )
            if (tempSet != null) {
                set.addAll(tempSet)
            }
            return set
        }
        set(set) {
            PrefsIoUtil.setString(
                R.string.preference_key_reading_list_pages_deleted_ids,
                GsonMarshaller.marshal(set)
            )
        }

    fun addReadingListPagesDeletedIds(set: Set<String>) {
        val maxStoredIds = 256
        val currentSet = readingListPagesDeletedIds
        currentSet.addAll(set)
        readingListPagesDeletedIds = if (currentSet.size < maxStoredIds) currentSet else set.toMutableSet()
    }

    var shouldShowReadingListSyncEnablePrompt: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_reading_lists_sync_prompt, true)
        set(enabled) {
            PrefsIoUtil.setBoolean(R.string.preference_key_show_reading_lists_sync_prompt, enabled)
        }

    var isReadingListsFirstTimeSync: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_reading_lists_first_time_sync, true)
        set(value) {
            PrefsIoUtil.setBoolean(R.string.preference_key_reading_lists_first_time_sync, value)
        }

    var editingTextSizeExtra: Int
        get() = PrefsIoUtil.getInt(R.string.preference_key_editing_text_size_extra, 0)
        set(extra) {
            PrefsIoUtil.setInt(R.string.preference_key_editing_text_size_extra, extra)
        }

    var isMultilingualSearchTutorialEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_multilingual_search_tutorial_enabled,
            true
        )
        set(enabled) {
            PrefsIoUtil.setBoolean(
                R.string.preference_key_multilingual_search_tutorial_enabled,
                enabled
            )
        }

    var shouldShowRemoveChineseVariantPrompt: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_show_remove_chinese_variant_prompt,
            true
        )
        set(enabled) {
            PrefsIoUtil.setBoolean(
                R.string.preference_key_show_remove_chinese_variant_prompt,
                enabled
            )
        }

    var locallyKnownNotifications: MutableList<Long>
        get() {
            val list: MutableList<Long> = ArrayList()
            if (!PrefsIoUtil.contains(R.string.preference_key_locally_known_notifications)) {
                return list
            }
            val tempList = GsonUnmarshaller.unmarshal(
                object : TypeToken<ArrayList<Long>>() {},
                PrefsIoUtil.getString(R.string.preference_key_locally_known_notifications, null)
            )
            if (tempList != null) {
                list.addAll(tempList)
            }
            return list
        }
        set(list) {
            PrefsIoUtil.setString(
                R.string.preference_key_locally_known_notifications,
                GsonMarshaller.marshal(list)
            )
        }

    var remoteNotificationsSeenTime: String
        get() = PrefsIoUtil.getString(R.string.preference_key_remote_notifications_seen_time, "")!!
        set(seenTime) {
            PrefsIoUtil.setString(R.string.preference_key_remote_notifications_seen_time, seenTime)
        }

    var shouldShowHistoryOfflineArticlesToast: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_history_offline_articles_toast, true)
        set(showToast) {
            PrefsIoUtil.setBoolean(
                R.string.preference_key_history_offline_articles_toast,
                showToast
            )
        }

    var isLoggedOutInBackground: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_logged_out_in_background, false)
        set(loggedOut) {
            PrefsIoUtil.setBoolean(R.string.preference_key_logged_out_in_background, loggedOut)
        }

    var shouldShowDescriptionEditSuccessPrompt: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_show_description_edit_success_prompt,
            true
        )
        set(enabled) {
            PrefsIoUtil.setBoolean(
                R.string.preference_key_show_description_edit_success_prompt,
                enabled
            )
        }

    var suggestedEditsCountForSurvey: Int
        get() = PrefsIoUtil.getInt(R.string.preference_key_suggested_edits_count_for_survey, 0)
        set(count) {
            PrefsIoUtil.setInt(R.string.preference_key_suggested_edits_count_for_survey, count)
        }

    var isSuggestedEditsSurveyClicked: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_suggested_edits_survey_clicked,
            false
        )
        set(surveyClicked) {
            PrefsIoUtil.setBoolean(
                R.string.preference_key_suggested_edits_survey_clicked,
                surveyClicked
            )
        }

    var shouldShowSuggestedEditsSurvey: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_suggested_edits_survey, false)
        set(showSurvey) {
            PrefsIoUtil.setBoolean(R.string.preference_key_show_suggested_edits_survey, showSurvey)
        }

    var shouldShowSuggestedEditsTooltip: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_suggested_edits_tooltip, true)
        set(enabled) {
            PrefsIoUtil.setBoolean(R.string.preference_key_show_suggested_edits_tooltip, enabled)
        }

    var hasVisitedArticlePage: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_visited_article_page, false)
        set(visited) {
            PrefsIoUtil.setBoolean(R.string.preference_key_visited_article_page, visited)
        }

    var announcementShownDialogs: MutableSet<String>
        get() {
            val emptySet: MutableSet<String> = LinkedHashSet()
            if (!hasAnnouncementShownDialogs()) {
                return emptySet
            }
            val announcement = GsonUnmarshaller.unmarshal(
                emptySet.javaClass,
                PrefsIoUtil.getString(R.string.preference_key_announcement_shown_dialogs, null)
            )
            return announcement ?: emptySet
        }
        set(newAnnouncementIds) {
            val announcementIds = announcementShownDialogs
            announcementIds.addAll(newAnnouncementIds)
            PrefsIoUtil.setString(
                R.string.preference_key_announcement_shown_dialogs,
                GsonMarshaller.marshal(announcementIds)
            )
        }

    fun hasAnnouncementShownDialogs(): Boolean {
        return PrefsIoUtil.contains(R.string.preference_key_announcement_shown_dialogs)
    }

    fun resetAnnouncementShownDialogs() {
        PrefsIoUtil.remove(R.string.preference_key_announcement_shown_dialogs)
    }

    var watchlistDisabledLanguages: MutableSet<String>
        get() {
            val emptySet: MutableSet<String> = LinkedHashSet()
            if (!PrefsIoUtil.contains(R.string.preference_key_watchlist_disabled_langs)) {
                return emptySet
            }
            val codes = GsonUnmarshaller.unmarshal(
                emptySet.javaClass,
                PrefsIoUtil.getString(R.string.preference_key_watchlist_disabled_langs, null)
            )
            return codes ?: emptySet
        }
        set(langCodes) {
            val codes = announcementShownDialogs
            codes.addAll(langCodes)
            PrefsIoUtil.setString(
                R.string.preference_key_watchlist_disabled_langs,
                GsonMarshaller.marshal(langCodes)
            )
        }

    fun shouldMatchSystemTheme(): Boolean {
        return PrefsIoUtil.getBoolean(R.string.preference_key_match_system_theme, true)
    }

    fun setMatchSystemTheme(enabled: Boolean) {
        PrefsIoUtil.setBoolean(R.string.preference_key_match_system_theme, enabled)
    }

    var suggestedEditsPauseDate: Date
        get() {
            var date = Date(0)
            if (PrefsIoUtil.contains(R.string.preference_key_suggested_edits_pause_date)) {
                date = dbDateParse(
                    PrefsIoUtil.getString(
                        R.string.preference_key_suggested_edits_pause_date,
                        ""
                    )!!
                )
            }
            return date
        }
        set(date) {
            PrefsIoUtil.setString(
                R.string.preference_key_suggested_edits_pause_date,
                dbDateFormat(date)
            )
        }
    var suggestedEditsPauseReverts: Int
        get() = PrefsIoUtil.getInt(R.string.preference_key_suggested_edits_pause_reverts, 0)
        set(count) {
            PrefsIoUtil.setInt(R.string.preference_key_suggested_edits_pause_reverts, count)
        }

    @JvmStatic
    fun shouldOverrideSuggestedEditCounts(): Boolean {
        return PrefsIoUtil.getBoolean(
            R.string.preference_key_suggested_edits_override_counts,
            false
        )
    }

    @JvmStatic
    val overrideSuggestedEditCount: Int
        get() = PrefsIoUtil.getInt(R.string.preference_key_suggested_edits_override_edits, 0)
    @JvmStatic
    val overrideSuggestedRevertCount: Int
        get() = PrefsIoUtil.getInt(R.string.preference_key_suggested_edits_override_reverts, 0)
    var installReferrerAttempts: Int
        get() = PrefsIoUtil.getInt(R.string.preference_key_install_referrer_attempts, 0)
        set(attempts) {
            PrefsIoUtil.setInt(R.string.preference_key_install_referrer_attempts, attempts)
        }

    fun shouldShowImageTagsOnboarding(): Boolean {
        return PrefsIoUtil.getBoolean(R.string.preference_key_image_tags_onboarding_shown, true)
    }

    fun setShowImageTagsOnboarding(showOnboarding: Boolean) {
        PrefsIoUtil.setBoolean(R.string.preference_key_image_tags_onboarding_shown, showOnboarding)
    }

    var shouldShowImageZoomTooltip: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_image_zoom_tooltip_shown, true)
        set(show) {
            PrefsIoUtil.setBoolean(R.string.preference_key_image_zoom_tooltip_shown, show)
        }
    var isSuggestedEditsReactivationPassStageOne: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_suggested_edits_reactivation_pass_stage_one,
            true
        )
        set(pass) {
            PrefsIoUtil.setBoolean(
                R.string.preference_key_suggested_edits_reactivation_pass_stage_one,
                pass
            )
        }
    var temporaryWikitext: String?
        get() = PrefsIoUtil.getString(R.string.preference_key_temporary_wikitext_storage, null)
        set(wikitext) {
            PrefsIoUtil.setString(R.string.preference_key_temporary_wikitext_storage, wikitext)
        }
    @JvmStatic
    var pushNotificationToken: String
        get() = PrefsIoUtil.getString(R.string.preference_key_push_notification_token, "")!!
        set(token) {
            PrefsIoUtil.setString(R.string.preference_key_push_notification_token, token)
        }
    @JvmStatic
    var pushNotificationTokenOld: String
        get() = PrefsIoUtil.getString(R.string.preference_key_push_notification_token_old, "")!!
        set(token) {
            PrefsIoUtil.setString(R.string.preference_key_push_notification_token_old, token)
        }
    @JvmStatic
    var isPushNotificationTokenSubscribed: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_push_notification_token_subscribed,
            false
        )
        set(subscribed) {
            PrefsIoUtil.setBoolean(
                R.string.preference_key_push_notification_token_subscribed,
                subscribed
            )
        }
    val isSuggestedEditsReactivationTestEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_suggested_edits_reactivation_test,
            false
        )
    var isSuggestedEditsHighestPriorityEnabled: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_suggested_edits_highest_priority_enabled,
            false
        )
        set(enabled) {
            PrefsIoUtil.setBoolean(
                R.string.preference_key_suggested_edits_highest_priority_enabled,
                enabled
            )
        }

    fun incrementExploreFeedVisitCount() {
        PrefsIoUtil.setInt(
            R.string.preference_key_explore_feed_visit_count,
            exploreFeedVisitCount + 1
        )
    }

    val exploreFeedVisitCount: Int
        get() = PrefsIoUtil.getInt(R.string.preference_key_explore_feed_visit_count, 0)

    var selectedLanguagePositionInSearch: Int
        get() = PrefsIoUtil.getInt(R.string.preference_key_selected_language_position_in_search, 0)
        set(position) {
            PrefsIoUtil.setInt(
                R.string.preference_key_selected_language_position_in_search,
                position
            )
        }

    var shouldShowOneTimeSequentialUserStatsTooltip: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_show_sequential_user_stats_tooltip,
            true
        )
        set(show) {
            PrefsIoUtil.setBoolean(R.string.preference_key_show_sequential_user_stats_tooltip, show)
        }

    var showSearchTabTooltip: Boolean
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_search_tab_tooltip, true)
        set(show) {
            PrefsIoUtil.setBoolean(R.string.preference_key_show_search_tab_tooltip, show)
        }

    @JvmStatic
    var eventPlatformSessionId: String?
        get() = PrefsIoUtil.getString(R.string.preference_key_event_platform_session_id, null)
        set(sessionId) {
            PrefsIoUtil.setString(R.string.preference_key_event_platform_session_id, sessionId)
        }

    @JvmStatic
    val streamConfigs: Map<String, StreamConfig>
        get() {
            val streamConfigJson = PrefsIoUtil.getString(
                R.string.preference_key_event_platform_stored_stream_configs,
                "{}"
            )
            return GsonUnmarshaller.unmarshal(object : TypeToken<HashMap<String, StreamConfig>>() {},
                streamConfigJson)
        }

    @JvmStatic
    fun setStreamConfigs(streamConfigs: Map<String?, StreamConfig?>) {
        PrefsIoUtil.setString(
            R.string.preference_key_event_platform_stored_stream_configs,
            GsonMarshaller.marshal(streamConfigs)
        )
    }

    var localClassName: String?
        get() = PrefsIoUtil.getString(R.string.preference_key_crash_report_local_class_name, "")
        set(className) {
            PrefsIoUtil.setString(R.string.preference_key_crash_report_local_class_name, className)
        }
    var isWatchlistPageOnboardingTooltipShown: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_watchlist_page_onboarding_tooltip_shown,
            false
        )
        set(enabled) {
            PrefsIoUtil.setBoolean(
                R.string.preference_key_watchlist_page_onboarding_tooltip_shown,
                enabled
            )
        }
    var isWatchlistMainOnboardingTooltipShown: Boolean
        get() = PrefsIoUtil.getBoolean(
            R.string.preference_key_watchlist_main_onboarding_tooltip_shown,
            false
        )
        set(enabled) {
            PrefsIoUtil.setBoolean(
                R.string.preference_key_watchlist_main_onboarding_tooltip_shown,
                enabled
            )
        }
}
