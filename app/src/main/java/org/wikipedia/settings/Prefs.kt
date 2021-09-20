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
import org.wikipedia.theme.Theme.Companion.fallback
import org.wikipedia.util.DateUtil.dbDateFormat
import org.wikipedia.util.DateUtil.dbDateParse
import org.wikipedia.util.ReleaseUtil.isDevRelease
import java.util.*

/** Shared preferences utility for convenient POJO access.  */
object Prefs {

    var appChannel
        get() = PrefsIoUtil.getString(R.string.preference_key_app_channel, null)
        set(channel) = PrefsIoUtil.setString(R.string.preference_key_app_channel, channel)

    val appChannelKey
        get() = PrefsIoUtil.getKey(R.string.preference_key_app_channel)

    // The app install ID uses readingAppInstallID for backwards compatibility with analytics.
    var appInstallId
        get() = PrefsIoUtil.getString(R.string.preference_key_reading_app_install_id, null)
        set(id) = PrefsIoUtil.setString(R.string.preference_key_reading_app_install_id, id)

    var currentThemeId
        get() = PrefsIoUtil.getInt(R.string.preference_key_color_theme, fallback.marshallingId)
        set(theme) = PrefsIoUtil.setInt(R.string.preference_key_color_theme, theme)

    var previousThemeId
        get() = PrefsIoUtil.getInt(R.string.preference_key_previous_color_theme, fallback.marshallingId)
        set(theme) = PrefsIoUtil.setInt(R.string.preference_key_previous_color_theme, theme)

    var fontFamily
        get() = PrefsIoUtil.getString(R.string.preference_key_font_family, "").orEmpty().ifEmpty { "sans-serif" }
        set(fontFamily) = PrefsIoUtil.setString(R.string.preference_key_font_family, fontFamily)

    var cookies
        get() = if (!PrefsIoUtil.contains(R.string.preference_key_cookie_map)) {
            null
        } else GsonUnmarshaller.unmarshal(
            SharedPreferenceCookieManager::class.java,
            PrefsIoUtil.getString(R.string.preference_key_cookie_map, null)
        )
        set(value) = PrefsIoUtil.setString(R.string.preference_key_cookie_map, GsonMarshaller.marshal(value))

    var isShowDeveloperSettingsEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_developer_settings, isDevRelease)
        set(enabled) = PrefsIoUtil.setBoolean(R.string.preference_key_show_developer_settings, enabled)

    var mruLanguageCodeCsv
        get() = PrefsIoUtil.getString(R.string.preference_key_language_mru, null)
        set(csv) = PrefsIoUtil.setString(R.string.preference_key_language_mru, csv)

    var appLanguageCodeCsv
        get() = PrefsIoUtil.getString(R.string.preference_key_language_app, null)
        set(csv) = PrefsIoUtil.setString(R.string.preference_key_language_app, csv)

    var remoteConfigJson
        get() = PrefsIoUtil.getString(R.string.preference_key_remote_config, "").orEmpty().ifEmpty { "{}" }
        set(json) = PrefsIoUtil.setString(R.string.preference_key_remote_config, json)

    var tabs
        get() = if (hasTabs) TabUnmarshaller.unmarshal(PrefsIoUtil.getString(R.string.preference_key_tabs, null)) else emptyList()
        set(tabs) = PrefsIoUtil.setString(R.string.preference_key_tabs, GsonMarshaller.marshal(tabs))

    val hasTabs get() = PrefsIoUtil.contains(R.string.preference_key_tabs)

    fun clearTabs() {
        PrefsIoUtil.remove(R.string.preference_key_tabs)
    }

    var hiddenCards: Set<String>
        get() {
            val emptySet = linkedSetOf<String>()
            if (!hasHiddenCards()) {
                return emptySet
            }
            val cards = GsonUnmarshaller.unmarshal(emptySet.javaClass, PrefsIoUtil.getString(R.string.preference_key_feed_hidden_cards, null))
            return cards ?: emptySet
        }
        set(cards) = PrefsIoUtil.setString(R.string.preference_key_feed_hidden_cards, GsonMarshaller.marshal(cards))

    private fun hasHiddenCards(): Boolean {
        return PrefsIoUtil.contains(R.string.preference_key_feed_hidden_cards)
    }

    var sessionData
        get() = if (hasSessionData()) SessionUnmarshaller.unmarshal(
            PrefsIoUtil.getString(
                R.string.preference_key_session_data,
                null
            )
        ) else SessionData()
        set(data) = PrefsIoUtil.setString(R.string.preference_key_session_data, GsonMarshaller.marshal(data))

    private fun hasSessionData(): Boolean {
        return PrefsIoUtil.contains(R.string.preference_key_session_data)
    }

    // return the timeout, but don't let it be less than the minimum
    val sessionTimeout
        get() = PrefsIoUtil.getInt(
            R.string.preference_key_session_timeout,
            SessionFunnel.DEFAULT_SESSION_TIMEOUT
        ).coerceAtLeast(SessionFunnel.MIN_SESSION_TIMEOUT)

    var textSizeMultiplier
        get() = PrefsIoUtil.getInt(R.string.preference_key_text_size_multiplier, 0)
        set(multiplier) = PrefsIoUtil.setInt(R.string.preference_key_text_size_multiplier, multiplier)

    var isEventLoggingEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_eventlogging_opt_in, true)
        set(enabled) = PrefsIoUtil.setBoolean(R.string.preference_key_eventlogging_opt_in, enabled)

    val announcementsCountryOverride
        get() = PrefsIoUtil.getString(R.string.preference_key_announcement_country_override, null)

    val ignoreDateForAnnouncements
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_announcement_ignore_date, false)

    val announcementsVersionCode
        get() = PrefsIoUtil.getInt(R.string.preference_key_announcement_version_code, 0)

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

    val restbaseUriFormat
        get() = PrefsIoUtil.getString(R.string.preference_key_restbase_uri_format, null)
            .orEmpty().ifEmpty { BuildConfig.DEFAULT_RESTBASE_URI_FORMAT }

    val mediaWikiBaseUrl
        get() = PrefsIoUtil.getString(R.string.preference_key_mediawiki_base_uri, "")!!

    val mediaWikiBaseUriSupportsLangCode
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_mediawiki_base_uri_supports_lang_code, true)

    val eventPlatformIntakeUriOverride
        get() = PrefsIoUtil.getString(R.string.preference_key_event_platform_intake_base_uri, "")!!

    fun getLastRunTime(task: String): Long {
        return PrefsIoUtil.getLong(getLastRunTimeKey(task), 0)
    }

    fun setLastRunTime(task: String, time: Long) {
        PrefsIoUtil.setLong(getLastRunTimeKey(task), time)
    }

    private fun getLastRunTimeKey(task: String): String {
        return PrefsIoUtil.getKey(R.string.preference_key_last_run_time_format, task)
    }

    var pageLastShown
        get() = PrefsIoUtil.getLong(R.string.preference_key_page_last_shown, 0)
        set(value) = PrefsIoUtil.setLong(R.string.preference_key_page_last_shown, value)

    val isImageDownloadEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_images, true)

    val isDownloadOnlyOverWiFiEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_download_only_over_wifi, false)

    val isDownloadingReadingListArticlesEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_download_reading_list_articles, true)

    val isLinkPreviewEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_link_previews, true)

    val isCollapseTablesEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_collapse_tables, true)

    fun getReadingListSortMode(defaultValue: Int): Int {
        return PrefsIoUtil.getInt(R.string.preference_key_reading_list_sort_mode, defaultValue)
    }

    fun setReadingListSortMode(sortMode: Int) {
        PrefsIoUtil.setInt(R.string.preference_key_reading_list_sort_mode, sortMode)
    }

    var readingListsPageSaveCount
        get() = PrefsIoUtil.getInt(R.string.preference_key_save_count_reading_lists, 0)
        set(saveCount) = PrefsIoUtil.setInt(R.string.preference_key_save_count_reading_lists, saveCount)

    fun getReadingListPageSortMode(defaultValue: Int): Int {
        return PrefsIoUtil.getInt(R.string.preference_key_reading_list_page_sort_mode, defaultValue)
    }

    fun setReadingListPageSortMode(sortMode: Int) {
        PrefsIoUtil.setInt(R.string.preference_key_reading_list_page_sort_mode, sortMode)
    }

    val isMemoryLeakTestEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_memory_leak_test, false)

    var isDescriptionEditTutorialEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_description_edit_tutorial_enabled, true)
        set(enabled) = PrefsIoUtil.setBoolean(R.string.preference_key_description_edit_tutorial_enabled, enabled)

    var lastDescriptionEditTime
        get() = PrefsIoUtil.getLong(R.string.preference_key_last_description_edit_time, 0)
        set(time) = PrefsIoUtil.setLong(R.string.preference_key_last_description_edit_time, time)

    val totalAnonDescriptionsEdited
        get() = PrefsIoUtil.getInt(R.string.preference_key_total_anon_descriptions_edited, 0)

    fun incrementTotalAnonDescriptionsEdited() {
        PrefsIoUtil.setInt(R.string.preference_key_total_anon_descriptions_edited, totalAnonDescriptionsEdited + 1)
    }

    var isReadingListSyncEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_sync_reading_lists, false)
        set(enabled) = PrefsIoUtil.setBoolean(R.string.preference_key_sync_reading_lists, enabled)

    var isReadingListSyncReminderEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_reading_list_sync_reminder_enabled, true)
        set(enabled) = PrefsIoUtil.setBoolean(R.string.preference_key_reading_list_sync_reminder_enabled, enabled)

    var isReadingListLoginReminderEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_reading_list_login_reminder_enabled, true)
        set(enabled) = PrefsIoUtil.setBoolean(R.string.preference_key_reading_list_login_reminder_enabled, enabled)

    var isReadingListsRemoteDeletePending
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_reading_lists_remote_delete_pending, false)
        set(pending) = PrefsIoUtil.setBoolean(R.string.preference_key_reading_lists_remote_delete_pending, pending)

    var isReadingListsRemoteSetupPending
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_reading_lists_remote_setup_pending, false)
        set(pending) = PrefsIoUtil.setBoolean(R.string.preference_key_reading_lists_remote_setup_pending, pending)

    var isInitialOnboardingEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_initial_onboarding_enabled, true)
        set(enabled) = PrefsIoUtil.setBoolean(R.string.preference_key_initial_onboarding_enabled, enabled)

    fun askedForPermissionOnce(permission: String): Boolean {
        return PrefsIoUtil.getBoolean(R.string.preference_key_permission_asked.toString() + permission, false)
    }

    fun setAskedForPermissionOnce(permission: String) {
        PrefsIoUtil.setBoolean(R.string.preference_key_permission_asked.toString() + permission, true)
    }

    var dimDarkModeImages
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_dim_dark_mode_images, true)
        set(value) = PrefsIoUtil.setBoolean(R.string.preference_key_dim_dark_mode_images, value)

    var notificationUnreadCount
        get() = PrefsIoUtil.getInt(R.string.preference_key_notification_unread_count, 0)
        set(count) = PrefsIoUtil.setInt(R.string.preference_key_notification_unread_count, count)

    fun preferOfflineContent(): Boolean {
        return PrefsIoUtil.getBoolean(R.string.preference_key_prefer_offline_content, false)
    }

    var feedCardsEnabled: List<Boolean>
        get() {
            if (!PrefsIoUtil.contains(R.string.preference_key_feed_cards_enabled)) {
                return emptyList()
            }
            val enabledList = GsonUnmarshaller.unmarshal(
                object : TypeToken<ArrayList<Boolean>?>() {},
                PrefsIoUtil.getString(R.string.preference_key_feed_cards_enabled, null)
            )
            return enabledList ?: emptyList()
        }
        set(enabledList) {
            PrefsIoUtil.setString(R.string.preference_key_feed_cards_enabled, GsonMarshaller.marshal(enabledList))
        }

    var feedCardsOrder: List<Int>
        get() {
            if (!PrefsIoUtil.contains(R.string.preference_key_feed_cards_order)) {
                return emptyList()
            }
            val orderList = GsonUnmarshaller.unmarshal(
                object : TypeToken<ArrayList<Int>?>() {},
                PrefsIoUtil.getString(R.string.preference_key_feed_cards_order, null)
            )
            return orderList ?: emptyList()
        }
        set(orderList) {
            PrefsIoUtil.setString(R.string.preference_key_feed_cards_order, GsonMarshaller.marshal(orderList))
        }

    val feedCardsLangSupported: Map<Int, List<String>>
        get() {
            if (!PrefsIoUtil.contains(R.string.preference_key_feed_cards_lang_supported)) {
                return emptyMap()
            }
            val map = GsonUnmarshaller.unmarshal(
                object : TypeToken<Map<Int, List<String>>?>() {},
                PrefsIoUtil.getString(R.string.preference_key_feed_cards_lang_supported, null)
            )
            return map ?: emptyMap()
        }

    fun setFeedCardsLangSupported(langSupportedMap: Map<Int, List<String>>) {
        PrefsIoUtil.setString(
            R.string.preference_key_feed_cards_lang_supported,
            GsonMarshaller.marshal(langSupportedMap)
        )
    }

    val feedCardsLangDisabled: Map<Int, List<String>>
        get() {
            if (!PrefsIoUtil.contains(R.string.preference_key_feed_cards_lang_disabled)) {
                return emptyMap()
            }
            val map = GsonUnmarshaller.unmarshal(
                object : TypeToken<Map<Int, List<String>>?>() {},
                PrefsIoUtil.getString(R.string.preference_key_feed_cards_lang_disabled, null)
            )
            return map ?: emptyMap()
        }

    fun setFeedCardsLangDisabled(langDisabledMap: Map<Int, List<String>>) {
        PrefsIoUtil.setString(
            R.string.preference_key_feed_cards_lang_disabled,
            GsonMarshaller.marshal(langDisabledMap)
        )
    }

    fun resetFeedCustomizations() {
        PrefsIoUtil.remove(R.string.preference_key_feed_hidden_cards)
        PrefsIoUtil.remove(R.string.preference_key_feed_cards_enabled)
        PrefsIoUtil.remove(R.string.preference_key_feed_cards_order)
        PrefsIoUtil.remove(R.string.preference_key_feed_cards_lang_disabled)
    }

    var readingListsLastSyncTime
        get() = PrefsIoUtil.getString(R.string.preference_key_reading_lists_last_sync_time, "")
        set(timeStr) = PrefsIoUtil.setString(R.string.preference_key_reading_lists_last_sync_time, timeStr)

    var readingListsDeletedIds: Set<Long>
        get() {
            val set = mutableSetOf<Long>()
            if (!PrefsIoUtil.contains(R.string.preference_key_reading_lists_deleted_ids)) {
                return set
            }
            val tempSet = GsonUnmarshaller.unmarshal(
                object : TypeToken<Set<Long>?>() {},
                PrefsIoUtil.getString(R.string.preference_key_reading_lists_deleted_ids, null)
            )
            if (tempSet != null) {
                set.addAll(tempSet)
            }
            return set
        }
        set(set) = PrefsIoUtil.setString(R.string.preference_key_reading_lists_deleted_ids, GsonMarshaller.marshal(set))

    fun addReadingListsDeletedIds(set: Set<Long>) {
        val maxStoredIds = 256
        val currentSet = readingListsDeletedIds.toMutableSet()
        currentSet.addAll(set)
        readingListsDeletedIds = if (currentSet.size < maxStoredIds) currentSet else set
    }

    var readingListPagesDeletedIds: Set<String>
        get() {
            val set = mutableSetOf<String>()
            if (!PrefsIoUtil.contains(R.string.preference_key_reading_lists_deleted_ids)) {
                return set
            }
            val tempSet = GsonUnmarshaller.unmarshal(
                object : TypeToken<Set<String>?>() {},
                PrefsIoUtil.getString(R.string.preference_key_reading_list_pages_deleted_ids, null)
            )
            if (tempSet != null) {
                set.addAll(tempSet)
            }
            return set
        }
        set(set) = PrefsIoUtil.setString(R.string.preference_key_reading_list_pages_deleted_ids, GsonMarshaller.marshal(set))

    fun addReadingListPagesDeletedIds(set: Set<String>) {
        val maxStoredIds = 256
        val currentSet = readingListPagesDeletedIds.toMutableSet()
        currentSet.addAll(set)
        readingListPagesDeletedIds = if (currentSet.size < maxStoredIds) currentSet else set
    }

    var showReadingListSyncEnablePrompt
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_reading_lists_sync_prompt, true)
        set(value) = PrefsIoUtil.setBoolean(R.string.preference_key_show_reading_lists_sync_prompt, value)

    var isReadingListsFirstTimeSync
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_reading_lists_first_time_sync, true)
        set(value) = PrefsIoUtil.setBoolean(R.string.preference_key_reading_lists_first_time_sync, value)

    var editingTextSizeExtra
        get() = PrefsIoUtil.getInt(R.string.preference_key_editing_text_size_extra, 0)
        set(extra) = PrefsIoUtil.setInt(R.string.preference_key_editing_text_size_extra, extra)

    var isMultilingualSearchTutorialEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_multilingual_search_tutorial_enabled, true)
        set(enabled) = PrefsIoUtil.setBoolean(R.string.preference_key_multilingual_search_tutorial_enabled, enabled)

    var shouldShowRemoveChineseVariantPrompt
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_remove_chinese_variant_prompt, true)
        set(enabled) = PrefsIoUtil.setBoolean(R.string.preference_key_show_remove_chinese_variant_prompt, enabled)

    var locallyKnownNotifications: List<Long>
        get() {
            val list = mutableListOf<Long>()
            if (!PrefsIoUtil.contains(R.string.preference_key_locally_known_notifications)) {
                return list
            }
            val tempList = GsonUnmarshaller.unmarshal(
                object : TypeToken<ArrayList<Long>?>() {},
                PrefsIoUtil.getString(R.string.preference_key_locally_known_notifications, null)
            )
            if (tempList != null) {
                list.addAll(tempList)
            }
            return list
        }
        set(list) = PrefsIoUtil.setString(R.string.preference_key_locally_known_notifications, GsonMarshaller.marshal(list))

    var remoteNotificationsSeenTime
        get() = PrefsIoUtil.getString(R.string.preference_key_remote_notifications_seen_time, "").orEmpty()
        set(seenTime) = PrefsIoUtil.setString(R.string.preference_key_remote_notifications_seen_time, seenTime)

    var showHistoryOfflineArticlesToast
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_history_offline_articles_toast, true)
        set(value) = PrefsIoUtil.setBoolean(R.string.preference_key_history_offline_articles_toast, value)

    var loggedOutInBackground
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_logged_out_in_background, false)
        set(value) = PrefsIoUtil.setBoolean(R.string.preference_key_logged_out_in_background, value)

    var showDescriptionEditSuccessPrompt
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_description_edit_success_prompt, true)
        set(value) = PrefsIoUtil.setBoolean(R.string.preference_key_show_description_edit_success_prompt, value)

    var suggestedEditsCountForSurvey
        get() = PrefsIoUtil.getInt(R.string.preference_key_suggested_edits_count_for_survey, 0)
        set(count) = PrefsIoUtil.setInt(R.string.preference_key_suggested_edits_count_for_survey, count)

    var suggestedEditsSurveyClicked
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_suggested_edits_survey_clicked, false)
        set(value) = PrefsIoUtil.setBoolean(R.string.preference_key_suggested_edits_survey_clicked, value)

    var showSuggestedEditsSurvey
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_suggested_edits_survey, false)
        set(value) = PrefsIoUtil.setBoolean(R.string.preference_key_show_suggested_edits_survey, value)

    var showSuggestedEditsTooltip
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_suggested_edits_tooltip, true)
        set(value) = PrefsIoUtil.setBoolean(R.string.preference_key_show_suggested_edits_tooltip, value)

    var hasVisitedArticlePage
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_suggested_edits_tooltip, false)
        set(value) = PrefsIoUtil.setBoolean(R.string.preference_key_show_suggested_edits_tooltip, value)

    var announcementShownDialogs: Set<String>
        get() {
            val emptySet = linkedSetOf<String>()
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
            val announcementIds = announcementShownDialogs.toMutableList()
            announcementIds.addAll(newAnnouncementIds)
            PrefsIoUtil.setString(
                R.string.preference_key_announcement_shown_dialogs,
                GsonMarshaller.marshal(announcementIds)
            )
        }

    private fun hasAnnouncementShownDialogs(): Boolean {
        return PrefsIoUtil.contains(R.string.preference_key_announcement_shown_dialogs)
    }

    fun resetAnnouncementShownDialogs() {
        PrefsIoUtil.remove(R.string.preference_key_announcement_shown_dialogs)
    }

    var watchlistDisabledLanguages: Set<String>
        get() {
            val emptySet = linkedSetOf<String>()
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
            val codes = watchlistDisabledLanguages.toMutableList()
            codes.addAll(langCodes)
            PrefsIoUtil.setString(R.string.preference_key_watchlist_disabled_langs, GsonMarshaller.marshal(langCodes))
        }

    var shouldMatchSystemTheme
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_match_system_theme, true)
        set(value) = PrefsIoUtil.setBoolean(R.string.preference_key_match_system_theme, value)

    var suggestedEditsPauseDate: Date
        get() {
            var date = Date(0)
            if (PrefsIoUtil.contains(R.string.preference_key_suggested_edits_pause_date)) {
                date = dbDateParse(PrefsIoUtil.getString(
                        R.string.preference_key_suggested_edits_pause_date,
                        ""
                    )!!)
            }
            return date
        }
        set(date) = PrefsIoUtil.setString(R.string.preference_key_suggested_edits_pause_date, dbDateFormat(date))

    var suggestedEditsPauseReverts
        get() = PrefsIoUtil.getInt(R.string.preference_key_suggested_edits_pause_reverts, 0)
        set(count) = PrefsIoUtil.setInt(R.string.preference_key_suggested_edits_pause_reverts, count)

    fun shouldOverrideSuggestedEditCounts(): Boolean {
        return PrefsIoUtil.getBoolean(R.string.preference_key_suggested_edits_override_counts, false)
    }

    val overrideSuggestedEditCount
        get() = PrefsIoUtil.getInt(R.string.preference_key_suggested_edits_override_edits, 0)

    val overrideSuggestedRevertCount
        get() = PrefsIoUtil.getInt(R.string.preference_key_suggested_edits_override_reverts, 0)

    var installReferrerAttempts
        get() = PrefsIoUtil.getInt(R.string.preference_key_install_referrer_attempts, 0)
        set(attempts) = PrefsIoUtil.setInt(R.string.preference_key_install_referrer_attempts, attempts)

    var showImageTagsOnboarding
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_image_tags_onboarding_shown, true)
        set(value) = PrefsIoUtil.setBoolean(R.string.preference_key_image_tags_onboarding_shown, value)

    var showImageZoomTooltip
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_image_zoom_tooltip_shown, true)
        set(value) = PrefsIoUtil.setBoolean(R.string.preference_key_image_zoom_tooltip_shown, value)

    var isSuggestedEditsReactivationPassStageOne
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_suggested_edits_reactivation_pass_stage_one, true)
        set(pass) = PrefsIoUtil.setBoolean(R.string.preference_key_suggested_edits_reactivation_pass_stage_one, pass)

    var temporaryWikitext
        get() = PrefsIoUtil.getString(R.string.preference_key_temporary_wikitext_storage, "")
        set(value) = PrefsIoUtil.setString(R.string.preference_key_temporary_wikitext_storage, value)

    var pushNotificationToken
        get() = PrefsIoUtil.getString(R.string.preference_key_push_notification_token, "").orEmpty()
        set(token) = PrefsIoUtil.setString(R.string.preference_key_push_notification_token, token)

    var pushNotificationTokenOld
        get() = PrefsIoUtil.getString(R.string.preference_key_push_notification_token_old, "").orEmpty()
        set(token) = PrefsIoUtil.setString(R.string.preference_key_push_notification_token_old, token)

    var isPushNotificationTokenSubscribed
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_push_notification_token_subscribed, false)
        set(subscribed) = PrefsIoUtil.setBoolean(R.string.preference_key_push_notification_token_subscribed, subscribed)

    val isSuggestedEditsReactivationTestEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_suggested_edits_reactivation_test, false)

    var isSuggestedEditsHighestPriorityEnabled
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_suggested_edits_highest_priority_enabled, false)
        set(enabled) = PrefsIoUtil.setBoolean(R.string.preference_key_suggested_edits_highest_priority_enabled, enabled)

    fun incrementExploreFeedVisitCount() {
        PrefsIoUtil.setInt(R.string.preference_key_explore_feed_visit_count, exploreFeedVisitCount + 1)
    }

    val exploreFeedVisitCount
        get() = PrefsIoUtil.getInt(R.string.preference_key_explore_feed_visit_count, 0)

    var selectedLanguagePositionInSearch
        get() = PrefsIoUtil.getInt(R.string.preference_key_selected_language_position_in_search, 0)
        set(position) = PrefsIoUtil.setInt(R.string.preference_key_selected_language_position_in_search, position)

    var showOneTimeSequentialUserStatsTooltip
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_sequential_user_stats_tooltip, true)
        set(value) = PrefsIoUtil.setBoolean(R.string.preference_key_show_sequential_user_stats_tooltip, value)

    var showSearchTabTooltip
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_show_search_tab_tooltip, true)
        set(value) = PrefsIoUtil.setBoolean(R.string.preference_key_show_search_tab_tooltip, value)

    var eventPlatformSessionId
        get() = PrefsIoUtil.getString(R.string.preference_key_event_platform_session_id, null)
        set(sessionId) = PrefsIoUtil.setString(R.string.preference_key_event_platform_session_id, sessionId)

    val streamConfigs: Map<String, StreamConfig>
        get() {
            val streamConfigJson = PrefsIoUtil.getString(R.string.preference_key_event_platform_stored_stream_configs, "").orEmpty().ifEmpty { "{}" }
            return GsonUnmarshaller.unmarshal(
                object : TypeToken<HashMap<String, StreamConfig>?>() {},
                streamConfigJson
            ) as HashMap<String, StreamConfig>
        }

    fun setStreamConfigs(streamConfigs: Map<String?, StreamConfig?>) {
        PrefsIoUtil.setString(
            R.string.preference_key_event_platform_stored_stream_configs,
            GsonMarshaller.marshal(streamConfigs)
        )
    }

    var localClassName
        get() = PrefsIoUtil.getString(R.string.preference_key_crash_report_local_class_name, "")
        set(className) = PrefsIoUtil.setString(R.string.preference_key_crash_report_local_class_name, className)

    var isWatchlistPageOnboardingTooltipShown
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_watchlist_page_onboarding_tooltip_shown, false)
        set(enabled) = PrefsIoUtil.setBoolean(R.string.preference_key_watchlist_page_onboarding_tooltip_shown, enabled)

    var isWatchlistMainOnboardingTooltipShown
        get() = PrefsIoUtil.getBoolean(R.string.preference_key_watchlist_main_onboarding_tooltip_shown, false)
        set(enabled) = PrefsIoUtil.setBoolean(R.string.preference_key_watchlist_main_onboarding_tooltip_shown, enabled)
}
