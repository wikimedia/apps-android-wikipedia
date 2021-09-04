package org.wikipedia.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Types;

import org.wikipedia.BuildConfig;
import org.wikipedia.R;
import org.wikipedia.analytics.SessionData;
import org.wikipedia.analytics.SessionFunnel;
import org.wikipedia.analytics.eventplatform.StreamConfig;
import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.json.MoshiUtil;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.theme.Theme;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.log.L;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.logging.HttpLoggingInterceptor.Level;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.wikipedia.settings.PrefsIoUtil.contains;
import static org.wikipedia.settings.PrefsIoUtil.getBoolean;
import static org.wikipedia.settings.PrefsIoUtil.getInt;
import static org.wikipedia.settings.PrefsIoUtil.getKey;
import static org.wikipedia.settings.PrefsIoUtil.getLong;
import static org.wikipedia.settings.PrefsIoUtil.getString;
import static org.wikipedia.settings.PrefsIoUtil.remove;
import static org.wikipedia.settings.PrefsIoUtil.setBoolean;
import static org.wikipedia.settings.PrefsIoUtil.setInt;
import static org.wikipedia.settings.PrefsIoUtil.setLong;
import static org.wikipedia.settings.PrefsIoUtil.setString;

/** Shared preferences utility for convenient POJO access. */
@SuppressWarnings("checkstyle:magicnumber")
public final class Prefs {
    @Nullable
    public static String getAppChannel() {
        return getString(R.string.preference_key_app_channel, null);
    }

    public static void setAppChannel(@Nullable String channel) {
        setString(R.string.preference_key_app_channel, channel);
    }

    @NonNull
    public static String getAppChannelKey() {
        return getKey(R.string.preference_key_app_channel);
    }

    @Nullable
    public static String getAppInstallId() {
        return getString(R.string.preference_key_reading_app_install_id, null);
    }

    public static void setAppInstallId(@Nullable String id) {
        // The app install ID uses readingAppInstallID for backwards compatibility with analytics.
        setString(R.string.preference_key_reading_app_install_id, id);
    }

    public static int getCurrentThemeId() {
        return getInt(R.string.preference_key_color_theme, Theme.getFallback().getMarshallingId());
    }

    public static void setCurrentThemeId(int theme) {
        setInt(R.string.preference_key_color_theme, theme);
    }

    public static int getPreviousThemeId() {
        return getInt(R.string.preference_key_previous_color_theme, Theme.getFallback().getMarshallingId());
    }

    public static void setPreviousThemeId(int theme) {
        setInt(R.string.preference_key_previous_color_theme, theme);
    }

    public static String getFontFamily() {
        return getString(R.string.preference_key_font_family, "sans-serif");
    }

    public static void setFontFamily(String fontFamily) {
        setString(R.string.preference_key_font_family, fontFamily);
    }

    public static void setCookies(@NonNull SharedPreferenceCookieManager cookies) {
        final JsonAdapter<SharedPreferenceCookieManager> adapter = MoshiUtil.getDefaultMoshi()
                .adapter(SharedPreferenceCookieManager.class);
        setString(R.string.preference_key_cookie_map, adapter.toJson(cookies));
    }

    @Nullable public static SharedPreferenceCookieManager getCookies() throws IOException {
        if (!contains(R.string.preference_key_cookie_map)) {
            return null;
        }
        final JsonAdapter<SharedPreferenceCookieManager> adapter = MoshiUtil.getDefaultMoshi()
                .adapter(SharedPreferenceCookieManager.class).nullSafe();
        return adapter.fromJson(getString(R.string.preference_key_cookie_map, "null"));
    }

    public static boolean crashedBeforeActivityCreated() {
        return getBoolean(R.string.preference_key_crashed_before_activity_created, true);
    }

    public static void crashedBeforeActivityCreated(boolean crashed) {
        setBoolean(R.string.preference_key_crashed_before_activity_created, crashed);
    }

    public static boolean isShowDeveloperSettingsEnabled() {
        return getBoolean(R.string.preference_key_show_developer_settings, ReleaseUtil.isDevRelease());
    }

    public static void setShowDeveloperSettingsEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_show_developer_settings, enabled);
    }

    @Nullable
    public static String getMruLanguageCodeCsv() {
        return getString(R.string.preference_key_language_mru, null);
    }

    public static void setMruLanguageCodeCsv(@Nullable String csv) {
        setString(R.string.preference_key_language_mru, csv);
    }

    @Nullable
    public static String getAppLanguageCodeCsv() {
        return getString(R.string.preference_key_language_app, null);
    }

    public static void setAppLanguageCodeCsv(@Nullable String csv) {
        setString(R.string.preference_key_language_app, csv);
    }

    @NonNull
    public static String getRemoteConfigJson() {
        return getString(R.string.preference_key_remote_config, "{}");
    }

    public static void setRemoteConfigJson(@Nullable String json) {
        setString(R.string.preference_key_remote_config, json);
    }

    public static void setTabs(@NonNull List<Tab> tabs) {
        final Type type = Types.newParameterizedType(List.class, Tab.class);
        final JsonAdapter<List<Tab>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        setString(R.string.preference_key_tabs, adapter.toJson(tabs));
    }

    @NonNull
    public static List<Tab> getTabs() {
        if (hasTabs()) {
            try {
                final Type type = Types.newParameterizedType(List.class, Tab.class);
                final JsonAdapter<List<Tab>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
                final List<Tab> tabs = adapter.fromJson(getString(R.string.preference_key_tabs, "null"));
                return tabs != null ? tabs : Collections.emptyList();
            } catch (IOException e) {
                L.e(e);
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

    public static boolean hasTabs() {
        return contains(R.string.preference_key_tabs);
    }

    public static void clearTabs() {
        remove(R.string.preference_key_tabs);
    }

    @NonNull public static Set<String> getHiddenCards() {
        if (!hasHiddenCards()) {
            return Collections.emptySet();
        }
        final Type type = Types.newParameterizedType(Set.class, String.class);
        final JsonAdapter<Set<String>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        try {
            final Set<String> cards = adapter.fromJson(getString(R.string.preference_key_feed_hidden_cards,
                    "null"));
            return cards != null ? new LinkedHashSet<>(cards) : Collections.emptySet();
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    public static void setHiddenCards(@NonNull Set<String> cards) {
        final Type type = Types.newParameterizedType(Set.class, String.class);
        final JsonAdapter<Set<String>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        setString(R.string.preference_key_feed_hidden_cards, adapter.toJson(cards));
    }

    public static boolean hasHiddenCards() {
        return contains(R.string.preference_key_feed_hidden_cards);
    }

    public static void setSessionData(@NonNull SessionData data) {
        final JsonAdapter<SessionData> adapter = MoshiUtil.getDefaultMoshi().adapter(SessionData.class);
        setString(R.string.preference_key_session_data, adapter.toJson(data));
    }

    @NonNull
    public static SessionData getSessionData() {
        if (hasSessionData()) {
            try {
                final JsonAdapter<SessionData> adapter = MoshiUtil.getDefaultMoshi().adapter(SessionData.class);
                final SessionData sessionData = adapter.fromJson(
                        getString(R.string.preference_key_session_data, "null"));
                return sessionData != null ? sessionData : new SessionData();
            } catch (IOException e) {
                L.e(e);
                return new SessionData();
            }
        } else {
            return new SessionData();
        }
    }

    public static boolean hasSessionData() {
        return contains(R.string.preference_key_session_data);
    }

    public static int getSessionTimeout() {
        // return the timeout, but don't let it be less than the minimum
        return Math.max(getInt(R.string.preference_key_session_timeout, SessionFunnel.DEFAULT_SESSION_TIMEOUT), SessionFunnel.MIN_SESSION_TIMEOUT);
    }

    public static int getTextSizeMultiplier() {
        return getInt(R.string.preference_key_text_size_multiplier, 0);
    }

    public static void setTextSizeMultiplier(int multiplier) {
        setInt(R.string.preference_key_text_size_multiplier, multiplier);
    }

    public static boolean isEventLoggingEnabled() {
        return getBoolean(R.string.preference_key_eventlogging_opt_in, true);
    }

    public static void setEventLoggingEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_eventlogging_opt_in, enabled);
    }

    public static String getAnnouncementsCountryOverride() {
        return getString(R.string.preference_key_announcement_country_override, null);
    }

    public static boolean ignoreDateForAnnouncements() {
        return getBoolean(R.string.preference_key_announcement_ignore_date, false);
    }

    public static int announcementsVersionCode() {
        return getInt(R.string.preference_key_announcement_version_code, 0);
    }

    public static Level getRetrofitLogLevel() {
        String prefValue = getString(R.string.preference_key_retrofit_log_level, null);
        if (prefValue == null) {
            return ReleaseUtil.isDevRelease() ? Level.BASIC : Level.NONE;
        }
        switch (prefValue) {
            case "BASIC":
                return Level.BASIC;
            case "HEADERS":
                return Level.HEADERS;
            case "BODY":
                return Level.BODY;
            case "NONE":
            default:
                return Level.NONE;
        }
    }

    @NonNull
    public static String getRestbaseUriFormat() {
        return defaultIfBlank(getString(R.string.preference_key_restbase_uri_format, null),
                BuildConfig.DEFAULT_RESTBASE_URI_FORMAT);
    }

    @NonNull
    public static String getMediaWikiBaseUrl() {
        return getString(R.string.preference_key_mediawiki_base_uri, "");
    }

    public static boolean getMediaWikiBaseUriSupportsLangCode() {
        return getBoolean(R.string.preference_key_mediawiki_base_uri_supports_lang_code, true);
    }

    @NonNull
    public static String getEventPlatformIntakeUriOverride() {
        return getString(R.string.preference_key_event_platform_intake_base_uri, "");
    }

    public static long getLastRunTime(@NonNull String task) {
        return getLong(getLastRunTimeKey(task), 0);
    }

    public static void setLastRunTime(@NonNull String task, long time) {
        setLong(getLastRunTimeKey(task), time);
    }

    public static long pageLastShown() {
        return getLong(R.string.preference_key_page_last_shown, 0);
    }

    public static void pageLastShown(long time) {
        setLong(R.string.preference_key_page_last_shown, time);
    }

    public static boolean isImageDownloadEnabled() {
        return getBoolean(R.string.preference_key_show_images, true);
    }

    public static boolean isDownloadOnlyOverWiFiEnabled() {
        return getBoolean(R.string.preference_key_download_only_over_wifi, false);
    }

    public static boolean isDownloadingReadingListArticlesEnabled() {
        return getBoolean(R.string.preference_key_download_reading_list_articles, true);
    }

    private static String getLastRunTimeKey(@NonNull String task) {
        return getKey(R.string.preference_key_last_run_time_format, task);
    }

    public static boolean isLinkPreviewEnabled() {
        return getBoolean(R.string.preference_key_show_link_previews, true);
    }

    public static boolean isCollapseTablesEnabled() {
        return getBoolean(R.string.preference_key_collapse_tables, true);
    }

    public static int getReadingListSortMode(int defaultValue) {
        return getInt(R.string.preference_key_reading_list_sort_mode, defaultValue);
    }

    public static void setReadingListSortMode(int sortMode) {
        setInt(R.string.preference_key_reading_list_sort_mode, sortMode);
    }

    public static int getReadingListsPageSaveCount() {
        return getInt(R.string.preference_key_save_count_reading_lists, 0);
    }

    public static void setReadingListsPageSaveCount(int saveCount) {
        setInt(R.string.preference_key_save_count_reading_lists, saveCount);
    }

    public static int getReadingListPageSortMode(int defaultValue) {
        return getInt(R.string.preference_key_reading_list_page_sort_mode, defaultValue);
    }

    public static void setReadingListPageSortMode(int sortMode) {
        setInt(R.string.preference_key_reading_list_page_sort_mode, sortMode);
    }

    public static boolean isMemoryLeakTestEnabled() {
        return getBoolean(R.string.preference_key_memory_leak_test, false);
    }

    public static boolean isDescriptionEditTutorialEnabled() {
        return getBoolean(R.string.preference_key_description_edit_tutorial_enabled, true);
    }

    public static void setDescriptionEditTutorialEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_description_edit_tutorial_enabled, enabled);
    }

    public static void setLastDescriptionEditTime(long time) {
        setLong(R.string.preference_key_last_description_edit_time, time);
    }

    public static long getLastDescriptionEditTime() {
        return getLong(R.string.preference_key_last_description_edit_time, 0);
    }

    public static int getTotalAnonDescriptionsEdited() {
        return getInt(R.string.preference_key_total_anon_descriptions_edited, 0);
    }

    public static void incrementTotalAnonDescriptionsEdited() {
        setInt(R.string.preference_key_total_anon_descriptions_edited, getTotalAnonDescriptionsEdited() + 1);
    }

    public static boolean isReadingListSyncEnabled() {
        return getBoolean(R.string.preference_key_sync_reading_lists, false);
    }

    public static void setReadingListSyncEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_sync_reading_lists, enabled);
    }

    public static boolean isReadingListSyncReminderEnabled() {
        return getBoolean(R.string.preference_key_reading_list_sync_reminder_enabled, true);
    }

    public static void setReadingListSyncReminderEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_reading_list_sync_reminder_enabled, enabled);
    }

    public static boolean isReadingListLoginReminderEnabled() {
        return getBoolean(R.string.preference_key_reading_list_login_reminder_enabled, true);
    }

    public static void setReadingListLoginReminderEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_reading_list_login_reminder_enabled, enabled);
    }

    public static boolean isReadingListsRemoteDeletePending() {
        return getBoolean(R.string.preference_key_reading_lists_remote_delete_pending, false);
    }

    public static void setReadingListsRemoteDeletePending(boolean pending) {
        setBoolean(R.string.preference_key_reading_lists_remote_delete_pending, pending);
    }

    public static boolean isReadingListsRemoteSetupPending() {
        return getBoolean(R.string.preference_key_reading_lists_remote_setup_pending, false);
    }

    public static void setReadingListsRemoteSetupPending(boolean pending) {
        setBoolean(R.string.preference_key_reading_lists_remote_setup_pending, pending);
    }

    public static boolean isInitialOnboardingEnabled() {
        return getBoolean(R.string.preference_key_initial_onboarding_enabled, true);
    }

    public static void setInitialOnboardingEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_initial_onboarding_enabled, enabled);
    }

    public static boolean askedForPermissionOnce(@NonNull String permission) {
        return getBoolean(R.string.preference_key_permission_asked + permission, false);
    }

    public static void setAskedForPermissionOnce(@NonNull String permission) {
        setBoolean(R.string.preference_key_permission_asked + permission, true);
    }

    public static boolean shouldDimDarkModeImages() {
        return getBoolean(R.string.preference_key_dim_dark_mode_images, true);
    }

    public static void setDimDarkModeImages(boolean enabled) {
        setBoolean(R.string.preference_key_dim_dark_mode_images, enabled);
    }

    public static int getNotificationUnreadCount() {
        return getInt(R.string.preference_key_notification_unread_count, 0);
    }

    public static void setNotificationUnreadCount(int count) {
        setInt(R.string.preference_key_notification_unread_count, count);
    }

    public static boolean preferOfflineContent() {
        return getBoolean(R.string.preference_key_prefer_offline_content, false);
    }

    @NonNull public static List<Boolean> getFeedCardsEnabled() {
        if (!contains(R.string.preference_key_feed_cards_enabled)) {
            return Collections.emptyList();
        }
        final Type type = Types.newParameterizedType(List.class, Boolean.class);
        final JsonAdapter<List<Boolean>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        try {
            final List<Boolean> enabledList = adapter.fromJson(
                    getString(R.string.preference_key_feed_cards_enabled, "null"));
            return enabledList != null ? enabledList : Collections.emptyList();
        } catch (IOException e) {
            L.e(e);
            return Collections.emptyList();
        }
    }

    public static void setFeedCardsEnabled(@NonNull List<Boolean> enabledList) {
        final Type type = Types.newParameterizedType(List.class, Boolean.class);
        final JsonAdapter<List<Boolean>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        setString(R.string.preference_key_feed_cards_enabled, adapter.toJson(enabledList));
    }

    @NonNull public static List<Integer> getFeedCardsOrder() {
        if (!contains(R.string.preference_key_feed_cards_order)) {
            return Collections.emptyList();
        }
        final Type type = Types.newParameterizedType(List.class, Integer.class);
        final JsonAdapter<List<Integer>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        try {
            final List<Integer> orderList = adapter
                    .fromJson(getString(R.string.preference_key_feed_cards_order, "null"));
            return orderList != null ? orderList : Collections.emptyList();
        } catch (IOException e) {
            L.e(e);
            return Collections.emptyList();
        }
    }

    public static void setFeedCardsOrder(@NonNull List<Integer> orderList) {
        final Type type = Types.newParameterizedType(List.class, Integer.class);
        final JsonAdapter<List<Integer>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        setString(R.string.preference_key_feed_cards_order, adapter.toJson(orderList));
    }

    @NonNull public static Map<Integer, List<String>> getFeedCardsLangSupported() {
        if (!contains(R.string.preference_key_feed_cards_lang_supported)) {
            return Collections.emptyMap();
        }
        final Type listType = Types.newParameterizedType(List.class, String.class);
        final Type mapType = Types.newParameterizedType(Map.class, Integer.class, listType);
        final JsonAdapter<Map<Integer, List<String>>> adapter = MoshiUtil.getDefaultMoshi().adapter(mapType);
        try {
            final Map<Integer, List<String>> map = adapter.fromJson(
                    getString(R.string.preference_key_feed_cards_lang_supported, "null"));
            return map != null ? map : Collections.emptyMap();
        } catch (IOException e) {
            L.e(e);
            return Collections.emptyMap();
        }
    }

    public static void setFeedCardsLangSupported(@NonNull Map<Integer, List<String>> langSupportedMap) {
        final Type listType = Types.newParameterizedType(List.class, String.class);
        final Type mapType = Types.newParameterizedType(Map.class, Integer.class, listType);
        final JsonAdapter<Map<Integer, List<String>>> adapter = MoshiUtil.getDefaultMoshi().adapter(mapType);
        setString(R.string.preference_key_feed_cards_lang_supported, adapter.toJson(langSupportedMap));
    }

    @NonNull public static Map<Integer, List<String>> getFeedCardsLangDisabled() {
        if (!contains(R.string.preference_key_feed_cards_lang_disabled)) {
            return Collections.emptyMap();
        }
        final Type listType = Types.newParameterizedType(List.class, String.class);
        final Type mapType = Types.newParameterizedType(Map.class, Integer.class, listType);
        final JsonAdapter<Map<Integer, List<String>>> adapter = MoshiUtil.getDefaultMoshi().adapter(mapType);
        try {
            final Map<Integer, List<String>> map = adapter.fromJson(
                    getString(R.string.preference_key_feed_cards_lang_disabled, "null"));
            return map != null ? map : Collections.emptyMap();
        } catch (IOException e) {
            L.e(e);
            return Collections.emptyMap();
        }
    }

    public static void setFeedCardsLangDisabled(@NonNull Map<Integer, List<String>> langDisabledMap) {
        final Type listType = Types.newParameterizedType(List.class, String.class);
        final Type mapType = Types.newParameterizedType(Map.class, Integer.class, listType);
        final JsonAdapter<Map<Integer, List<String>>> adapter = MoshiUtil.getDefaultMoshi().adapter(mapType);
        setString(R.string.preference_key_feed_cards_lang_disabled, adapter.toJson(langDisabledMap));
    }

    public static void resetFeedCustomizations() {
        remove(R.string.preference_key_feed_hidden_cards);
        remove(R.string.preference_key_feed_cards_enabled);
        remove(R.string.preference_key_feed_cards_order);
        remove(R.string.preference_key_feed_cards_lang_disabled);
    }

    public static String getReadingListsLastSyncTime() {
        return getString(R.string.preference_key_reading_lists_last_sync_time, "");
    }

    public static void setReadingListsLastSyncTime(@Nullable String timeStr) {
        setString(R.string.preference_key_reading_lists_last_sync_time, timeStr);
    }

    @NonNull public static Set<Long> getReadingListsDeletedIds() {
        if (!contains(R.string.preference_key_reading_lists_deleted_ids)) {
            return Collections.emptySet();
        }
        final Type type = Types.newParameterizedType(Set.class, Long.class);
        final JsonAdapter<Set<Long>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        try {
            final Set<Long> tempSet = adapter.fromJson(
                    getString(R.string.preference_key_reading_lists_deleted_ids, "null"));
            return tempSet != null ? tempSet : Collections.emptySet();
        } catch (IOException e) {
            L.e(e);
            return Collections.emptySet();
        }
    }

    public static void addReadingListsDeletedIds(@NonNull Set<Long> set) {
        final int maxStoredIds = 256;
        final Set<Long> currentSet = new HashSet<>(getReadingListsDeletedIds());
        currentSet.addAll(set);
        setReadingListsDeletedIds(currentSet.size() < maxStoredIds ? currentSet : set);
    }

    public static void setReadingListsDeletedIds(@NonNull Set<Long> set) {
        final Type type = Types.newParameterizedType(Set.class, Long.class);
        final JsonAdapter<Set<Long>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        setString(R.string.preference_key_reading_lists_deleted_ids, adapter.toJson(set));
    }

    @NonNull public static Set<String> getReadingListPagesDeletedIds() {
        if (!contains(R.string.preference_key_reading_lists_deleted_ids)) {
            return Collections.emptySet();
        }
        final Type type = Types.newParameterizedType(Set.class, String.class);
        final JsonAdapter<Set<String>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        try {
            final Set<String> tempSet = adapter.fromJson(
                    getString(R.string.preference_key_reading_list_pages_deleted_ids, "null"));
            return tempSet != null ? tempSet : Collections.emptySet();
        } catch (IOException e) {
            L.e(e);
            return Collections.emptySet();
        }
    }

    public static void addReadingListPagesDeletedIds(@NonNull Set<String> set) {
        final int maxStoredIds = 256;
        final Set<String> currentSet = new HashSet<>(getReadingListPagesDeletedIds());
        currentSet.addAll(set);
        setReadingListPagesDeletedIds(currentSet.size() < maxStoredIds ? currentSet : set);
    }

    public static void setReadingListPagesDeletedIds(@NonNull Set<String> set) {
        final Type type = Types.newParameterizedType(Set.class, String.class);
        final JsonAdapter<Set<String>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        setString(R.string.preference_key_reading_list_pages_deleted_ids, adapter.toJson(set));
    }

    public static boolean shouldShowReadingListSyncEnablePrompt() {
        return getBoolean(R.string.preference_key_show_reading_lists_sync_prompt, true);
    }

    public static void shouldShowReadingListSyncEnablePrompt(boolean enabled) {
        setBoolean(R.string.preference_key_show_reading_lists_sync_prompt, enabled);
    }

    public static boolean isReadingListsFirstTimeSync() {
        return getBoolean(R.string.preference_key_reading_lists_first_time_sync, true);
    }

    public static void setReadingListsFirstTimeSync(boolean value) {
        setBoolean(R.string.preference_key_reading_lists_first_time_sync, value);
    }

    public static int getEditingTextSizeExtra() {
        return getInt(R.string.preference_key_editing_text_size_extra, 0);
    }

    public static void setEditingTextSizeExtra(int extra) {
        setInt(R.string.preference_key_editing_text_size_extra, extra);
    }

    public static boolean isMultilingualSearchTutorialEnabled() {
        return getBoolean(R.string.preference_key_multilingual_search_tutorial_enabled, true);
    }

    public static void setMultilingualSearchTutorialEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_multilingual_search_tutorial_enabled, enabled);
    }

    public static void shouldShowRemoveChineseVariantPrompt(boolean enabled) {
        setBoolean(R.string.preference_key_show_remove_chinese_variant_prompt, enabled);
    }

    public static boolean shouldShowRemoveChineseVariantPrompt() {
        return getBoolean(R.string.preference_key_show_remove_chinese_variant_prompt, true);
    }

    @NonNull public static List<Long> getLocallyKnownNotifications() {
        if (!contains(R.string.preference_key_locally_known_notifications)) {
            return Collections.emptyList();
        }
        final Type type = Types.newParameterizedType(List.class, Long.class);
        final JsonAdapter<List<Long>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        try {
            final List<Long> enabledList = adapter.fromJson(
                    getString(R.string.preference_key_locally_known_notifications, "null"));
            return enabledList != null ? enabledList : Collections.emptyList();
        } catch (IOException e) {
            L.e(e);
            return Collections.emptyList();
        }
    }

    public static void setLocallyKnownNotifications(@NonNull List<Long> list) {
        final Type type = Types.newParameterizedType(List.class, Long.class);
        final JsonAdapter<List<Long>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        setString(R.string.preference_key_locally_known_notifications, adapter.toJson(list));
    }

    @NonNull
    public static Date getRemoteNotificationsSeenTime() {
        if (!contains(R.string.preference_key_remote_notifications_seen_time)) {
            return new Date(0);
        } else {
            return DateUtil.iso8601DateParse(getString(R.string.preference_key_remote_notifications_seen_time, ""));
        }
    }

    public static void setRemoteNotificationsSeenTime(@NonNull Date seenTime) {
        setString(R.string.preference_key_remote_notifications_seen_time, DateUtil.iso8601DateFormat(seenTime));
    }

    public static boolean shouldShowHistoryOfflineArticlesToast() {
        return getBoolean(R.string.preference_key_history_offline_articles_toast, true);
    }

    public static void shouldShowHistoryOfflineArticlesToast(boolean showToast) {
        setBoolean(R.string.preference_key_history_offline_articles_toast, showToast);
    }

    public static boolean wasLoggedOutInBackground() {
        return getBoolean(R.string.preference_key_logged_out_in_background, false);
    }

    public static void setLoggedOutInBackground(boolean loggedOut) {
        setBoolean(R.string.preference_key_logged_out_in_background, loggedOut);
    }

    public static boolean shouldShowDescriptionEditSuccessPrompt() {
        return getBoolean(R.string.preference_key_show_description_edit_success_prompt, true);
    }

    public static void shouldShowDescriptionEditSuccessPrompt(boolean enabled) {
        setBoolean(R.string.preference_key_show_description_edit_success_prompt, enabled);
    }

    public static int getSuggestedEditsCountForSurvey() {
        return getInt(R.string.preference_key_suggested_edits_count_for_survey, 0);
    }

    public static void setSuggestedEditsCountForSurvey(int count) {
        setInt(R.string.preference_key_suggested_edits_count_for_survey, count);
    }

    public static boolean wasSuggestedEditsSurveyClicked() {
        return getBoolean(R.string.preference_key_suggested_edits_survey_clicked, false);
    }

    public static void setSuggestedEditsSurveyClicked(boolean surveyClicked) {
        setBoolean(R.string.preference_key_suggested_edits_survey_clicked, surveyClicked);
    }

    public static boolean shouldShowSuggestedEditsSurvey() {
        return getBoolean(R.string.preference_key_show_suggested_edits_survey, false);
    }

    public static void setShouldShowSuggestedEditsSurvey(boolean showSurvey) {
        setBoolean(R.string.preference_key_show_suggested_edits_survey, showSurvey);
    }

    public static boolean shouldShowSuggestedEditsTooltip() {
        return getBoolean(R.string.preference_key_show_suggested_edits_tooltip, true);
    }

    public static void setShouldShowSuggestedEditsTooltip(boolean enabled) {
        setBoolean(R.string.preference_key_show_suggested_edits_tooltip, enabled);
    }

    public static boolean hasVisitedArticlePage() {
        return getBoolean(R.string.preference_key_visited_article_page, false);
    }

    public static void setHasVisitedArticlePage(boolean visited) {
        setBoolean(R.string.preference_key_visited_article_page, visited);
    }

    @NonNull public static Set<String> getAnnouncementShownDialogs() {
        if (!hasAnnouncementShownDialogs()) {
            return Collections.emptySet();
        }
        final Type type = Types.newParameterizedType(Set.class, String.class);
        final JsonAdapter<Set<String>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        try {
            final Set<String> cards = adapter.fromJson(
                    getString(R.string.preference_key_announcement_shown_dialogs, "null"));
            return cards != null ? cards : Collections.emptySet();
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    public static void setAnnouncementShownDialogs(@NonNull Set<String> newAnnouncementIds) {
        final Set<String> announcementIds = new LinkedHashSet<>(getAnnouncementShownDialogs());
        final Type type = Types.newParameterizedType(Set.class, String.class);
        final JsonAdapter<Set<String>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);

        announcementIds.addAll(newAnnouncementIds);
        setString(R.string.preference_key_announcement_shown_dialogs, adapter.toJson(announcementIds));
    }

    public static boolean hasAnnouncementShownDialogs() {
        return contains(R.string.preference_key_announcement_shown_dialogs);
    }

    public static void resetAnnouncementShownDialogs() {
        remove(R.string.preference_key_announcement_shown_dialogs);
    }

    @NonNull public static Set<String> getWatchlistDisabledLanguages() {
        if (!contains(R.string.preference_key_watchlist_disabled_langs)) {
            return Collections.emptySet();
        }
        final Type type = Types.newParameterizedType(Set.class, String.class);
        final JsonAdapter<Set<String>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        try {
            final Set<String> cards = adapter.fromJson(
                    getString(R.string.preference_key_watchlist_disabled_langs, null));
            return cards != null ? cards : Collections.emptySet();
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    public static void setWatchlistDisabledLanguages(@NonNull Set<String> langCodes) {
        final Set<String> codes = new LinkedHashSet<>(getAnnouncementShownDialogs());
        final Type type = Types.newParameterizedType(Set.class, String.class);
        final JsonAdapter<Set<String>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);

        codes.addAll(langCodes);
        setString(R.string.preference_key_watchlist_disabled_langs, adapter.toJson(codes));
    }

    public static boolean shouldMatchSystemTheme() {
        return getBoolean(R.string.preference_key_match_system_theme, true);
    }

    public static void setMatchSystemTheme(boolean enabled) {
        setBoolean(R.string.preference_key_match_system_theme, enabled);
    }

    public static Date getSuggestedEditsPauseDate() {
        Date date = new Date(0);
        if (contains(R.string.preference_key_suggested_edits_pause_date)) {
            date = DateUtil.dbDateParse(getString(R.string.preference_key_suggested_edits_pause_date, ""));
        }
        return date;
    }

    public static void setSuggestedEditsPauseDate(Date date) {
        setString(R.string.preference_key_suggested_edits_pause_date, DateUtil.dbDateFormat(date));
    }

    public static int getSuggestedEditsPauseReverts() {
        return getInt(R.string.preference_key_suggested_edits_pause_reverts, 0);
    }

    public static void setSuggestedEditsPauseReverts(int count) {
        setInt(R.string.preference_key_suggested_edits_pause_reverts, count);
    }

    public static boolean shouldOverrideSuggestedEditCounts() {
        return getBoolean(R.string.preference_key_suggested_edits_override_counts, false);
    }

    public static int getOverrideSuggestedEditCount() {
        return getInt(R.string.preference_key_suggested_edits_override_edits, 0);
    }

    public static int getOverrideSuggestedRevertCount() {
        return getInt(R.string.preference_key_suggested_edits_override_reverts, 0);
    }

    public static int getInstallReferrerAttempts() {
        return getInt(R.string.preference_key_install_referrer_attempts, 0);
    }

    public static void setInstallReferrerAttempts(int attempts) {
        setInt(R.string.preference_key_install_referrer_attempts, attempts);
    }

    public static boolean shouldShowImageTagsOnboarding() {
        return getBoolean(R.string.preference_key_image_tags_onboarding_shown, true);
    }

    public static void setShowImageTagsOnboarding(boolean showOnboarding) {
        setBoolean(R.string.preference_key_image_tags_onboarding_shown, showOnboarding);
    }

    public static boolean shouldShowImageZoomTooltip() {
        return getBoolean(R.string.preference_key_image_zoom_tooltip_shown, true);
    }

    public static void setShouldShowImageZoomTooltip(boolean show) {
        setBoolean(R.string.preference_key_image_zoom_tooltip_shown, show);
    }

    public static boolean isSuggestedEditsReactivationPassStageOne() {
        return getBoolean(R.string.preference_key_suggested_edits_reactivation_pass_stage_one, true);
    }

    public static void setSuggestedEditsReactivationPassStageOne(boolean pass) {
        setBoolean(R.string.preference_key_suggested_edits_reactivation_pass_stage_one, pass);
    }

    public static void storeTemporaryWikitext(@Nullable String wikitext) {
        setString(R.string.preference_key_temporary_wikitext_storage, wikitext);
    }

    public static String getTemporaryWikitext() {
        return getString(R.string.preference_key_temporary_wikitext_storage, "");
    }

    public static void setPushNotificationToken(@Nullable String token) {
        setString(R.string.preference_key_push_notification_token, token);
    }

    public static String getPushNotificationToken() {
        return getString(R.string.preference_key_push_notification_token, "");
    }

    public static void setPushNotificationTokenOld(@Nullable String token) {
        setString(R.string.preference_key_push_notification_token_old, token);
    }

    public static String getPushNotificationTokenOld() {
        return getString(R.string.preference_key_push_notification_token_old, "");
    }

    public static boolean isPushNotificationTokenSubscribed() {
        return getBoolean(R.string.preference_key_push_notification_token_subscribed, false);
    }

    public static void setPushNotificationTokenSubscribed(boolean subscribed) {
        setBoolean(R.string.preference_key_push_notification_token_subscribed, subscribed);
    }

    public static boolean isSuggestedEditsReactivationTestEnabled() {
        return getBoolean(R.string.preference_key_suggested_edits_reactivation_test, false);
    }

    public static boolean isSuggestedEditsHighestPriorityEnabled() {
        return getBoolean(R.string.preference_key_suggested_edits_highest_priority_enabled, false);
    }

    public static void setSuggestedEditsHighestPriorityEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_suggested_edits_highest_priority_enabled, enabled);
    }

    public static void incrementExploreFeedVisitCount() {
        setInt(R.string.preference_key_explore_feed_visit_count, getExploreFeedVisitCount() + 1);
    }

    public static int getExploreFeedVisitCount() {
        return getInt(R.string.preference_key_explore_feed_visit_count, 0);
    }

    public static int getSelectedLanguagePositionInSearch() {
        return getInt(R.string.preference_key_selected_language_position_in_search, 0);
    }

    public static void setSelectedLanguagePositionInSearch(int position) {
        setInt(R.string.preference_key_selected_language_position_in_search, position);
    }

    public static boolean shouldShowOneTimeSequentialUserStatsTooltip() {
        return getBoolean(R.string.preference_key_show_sequential_user_stats_tooltip, true);
    }

    public static void shouldShowOneTimeSequentialUserStatsTooltip(boolean show) {
        setBoolean(R.string.preference_key_show_sequential_user_stats_tooltip, show);
    }

    public static boolean shouldShowSearchTabTooltip() {
        return getBoolean(R.string.preference_key_show_search_tab_tooltip, true);
    }

    public static void setShowSearchTabTooltip(boolean show) {
        setBoolean(R.string.preference_key_show_search_tab_tooltip, show);
    }

    public static String getEventPlatformSessionId() {
        return getString(R.string.preference_key_event_platform_session_id, null);
    }

    public static void setEventPlatformSessionId(@Nullable String sessionId) {
        setString(R.string.preference_key_event_platform_session_id, sessionId);
    }

    public static Map<String, StreamConfig> getStreamConfigs() {
        final Type type = Types.newParameterizedType(Map.class, String.class, StreamConfig.class);
        final JsonAdapter<Map<String, StreamConfig>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        try {
            final Map<String, StreamConfig> map = adapter.fromJson(
                    getString(R.string.preference_key_event_platform_stored_stream_configs, "null"));
            return map != null ? map : Collections.emptyMap();
        } catch (IOException e) {
            L.e(e);
            return Collections.emptyMap();
        }
    }

    public static void setStreamConfigs(@NonNull Map<String, StreamConfig> streamConfigs) {
        final Type type = Types.newParameterizedType(Map.class, String.class, StreamConfig.class);
        final JsonAdapter<Map<String, StreamConfig>> adapter = MoshiUtil.getDefaultMoshi().adapter(type);
        setString(R.string.preference_key_event_platform_stored_stream_configs, adapter.toJson(streamConfigs));
    }

    public static void setLocalClassName(@Nullable String className) {
        setString(R.string.preference_key_crash_report_local_class_name, className);
    }

    public static String getLocalClassName() {
        return getString(R.string.preference_key_crash_report_local_class_name, "");
    }

    public static boolean isWatchlistPageOnboardingTooltipShown() {
        return getBoolean(R.string.preference_key_watchlist_page_onboarding_tooltip_shown, false);
    }

    public static void setWatchlistPageOnboardingTooltipShown(boolean enabled) {
        setBoolean(R.string.preference_key_watchlist_page_onboarding_tooltip_shown, enabled);
    }

    public static boolean isWatchlistMainOnboardingTooltipShown() {
        return getBoolean(R.string.preference_key_watchlist_main_onboarding_tooltip_shown, false);
    }

    public static void setWatchlistMainOnboardingTooltipShown(boolean enabled) {
        setBoolean(R.string.preference_key_watchlist_main_onboarding_tooltip_shown, enabled);
    }

    private Prefs() { }
}
