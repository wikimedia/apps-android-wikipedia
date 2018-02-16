package org.wikipedia.settings;

import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.analytics.SessionData;
import org.wikipedia.analytics.SessionFunnel;
import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.json.SessionUnmarshaller;
import org.wikipedia.json.TabUnmarshaller;
import org.wikipedia.offline.Compilation;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.theme.Theme;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import static org.wikipedia.settings.PrefsIoUtil.getStringSet;
import static org.wikipedia.settings.PrefsIoUtil.remove;
import static org.wikipedia.settings.PrefsIoUtil.setBoolean;
import static org.wikipedia.settings.PrefsIoUtil.setInt;
import static org.wikipedia.settings.PrefsIoUtil.setLong;
import static org.wikipedia.settings.PrefsIoUtil.setString;

/** Shared preferences utility for convenient POJO access. */
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

    @Nullable
    public static String getAppLanguageCode() {
        return getString(R.string.preference_key_language, null);
    }

    public static void setAppLanguageCode(@Nullable String code) {
        setString(R.string.preference_key_language, code);
    }

    public static int getThemeId() {
        return getInt(R.string.preference_key_color_theme, Theme.getFallback().getMarshallingId());
    }

    public static void setThemeId(int theme) {
        setInt(R.string.preference_key_color_theme, theme);
    }

    @NonNull
    public static String getCookieDomains() {
        return getString(R.string.preference_key_cookie_domains, "");
    }

    @NonNull
    public static List<String> getCookieDomainsAsList() {
        return SharedPreferenceCookieManager.makeList(getCookieDomains());
    }

    public static void setCookieDomains(@Nullable String domains) {
        setString(R.string.preference_key_cookie_domains, domains);
    }

    @NonNull
    public static String getCookiesForDomain(@NonNull String domain) {
        return getString(getCookiesForDomainKey(domain), "");
    }

    public static void setCookiesForDomain(@NonNull String domain, @Nullable String cookies) {
        setString(getCookiesForDomainKey(domain), cookies);
    }

    public static void removeCookiesForDomain(@NonNull String domain) {
        remove(getCookiesForDomainKey(domain));
    }

    public static boolean crashedBeforeActivityCreated() {
        return getBoolean(R.string.preference_key_crashed_before_activity_created, true);
    }

    public static void crashedBeforeActivityCreated(boolean crashed) {
        setBoolean(R.string.preference_key_crashed_before_activity_created, crashed);
    }

    public static boolean isCrashReportAutoUploadEnabled() {
        return getBoolean(R.string.preference_key_auto_upload_crash_reports, true);
    }

    public static boolean isShowDeveloperSettingsEnabled() {
        return getBoolean(R.string.preference_key_show_developer_settings, ReleaseUtil.isDevRelease());
    }

    public static void setShowDeveloperSettingsEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_show_developer_settings, enabled);
    }

    public static void removeLoginUsername() {
        remove(R.string.preference_key_login_username);
    }

    @Nullable
    public static String getLoginPassword() {
        return getString(R.string.preference_key_login_password, null);
    }

    public static boolean hasLoginPassword() {
        return contains(R.string.preference_key_login_password);
    }

    public static void removeLoginPassword() {
        remove(R.string.preference_key_login_password);
    }

    @NonNull
    public static Map<String, Integer> getLoginUserIds() {
        TypeToken<HashMap<String, Integer>> type = new TypeToken<HashMap<String, Integer>>(){};
        return GsonUnmarshaller.unmarshal(type, getString(R.string.preference_key_login_user_id_map, "{}"));
    }

    public static void removeLoginUserIds() {
        remove(R.string.preference_key_login_user_id_map);
    }

    @Nullable
    public static String getLoginUsername() {
        return getString(R.string.preference_key_login_username, null);
    }

    public static boolean hasLoginUsername() {
        return contains(R.string.preference_key_login_username);
    }

    @Nullable
    public static Set<String> getLoginGroups() {
        return getStringSet(R.string.preference_key_login_groups, null);
    }

    public static void removeLoginGroups() {
        remove(R.string.preference_key_login_groups);
    }

    @Nullable
    public static String getMruLanguageCodeCsv() {
        return getString(R.string.preference_key_language_mru, null);
    }

    public static void setMruLanguageCodeCsv(@Nullable String csv) {
        setString(R.string.preference_key_language_mru, csv);
    }

    @NonNull
    public static String getRemoteConfigJson() {
        return getString(R.string.preference_key_remote_config, "{}");
    }

    public static void setRemoteConfigJson(@Nullable String json) {
        setString(R.string.preference_key_remote_config, json);
    }

    public static void setTabs(@NonNull List<Tab> tabs) {
        setString(R.string.preference_key_tabs, GsonMarshaller.marshal(tabs));
    }

    @NonNull
    public static List<Tab> getTabs() {
        return hasTabs()
                ? TabUnmarshaller.unmarshal(getString(R.string.preference_key_tabs, null))
                : Collections.<Tab>emptyList();
    }

    public static boolean hasTabs() {
        return contains(R.string.preference_key_tabs);
    }

    public static void clearTabs() {
        remove(R.string.preference_key_tabs);
    }

    @NonNull public static Set<String> getHiddenCards() {
        Set<String> emptySet = new LinkedHashSet<>();
        if (!hasHiddenCards()) {
            return emptySet;
        }
        //noinspection unchecked
        Set<String> cards = GsonUnmarshaller.unmarshal(emptySet.getClass(),
                getString(R.string.preference_key_feed_hidden_cards, null));
        return cards != null ? cards : emptySet;
    }

    public static void setHiddenCards(@NonNull Set<String> cards) {
        setString(R.string.preference_key_feed_hidden_cards, GsonMarshaller.marshal(cards));
    }

    public static boolean hasHiddenCards() {
        return contains(R.string.preference_key_feed_hidden_cards);
    }

    public static int getTabCount() {
        List<Tab> tabs = getTabs();
        // handle the case where we have a single tab with an empty backstack,
        // which shouldn't count as a valid tab:
        return tabs.isEmpty() ? 0 : tabs.get(0).getBackStack().isEmpty() ? 0 : tabs.size();
    }

    public static void setSessionData(@NonNull SessionData data) {
        setString(R.string.preference_key_session_data, GsonMarshaller.marshal(data));
    }

    @NonNull
    public static SessionData getSessionData() {
        return hasSessionData()
                ? SessionUnmarshaller.unmarshal(getString(R.string.preference_key_session_data, null))
                : new SessionData();
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

    public static boolean useRestBaseSetManually() {
        return getBoolean(R.string.preference_key_use_restbase_manual, false);
    }

    public static boolean useRestBase() {
        return getBoolean(R.string.preference_key_use_restbase, true);
    }

    public static void setUseRestBase(boolean enabled) {
        setBoolean(R.string.preference_key_use_restbase, enabled);
    }

    public static int getRbTicket(int defaultValue) {
        return getInt(R.string.preference_key_restbase_ticket, defaultValue);
    }

    public static void setRbTicket(int rbTicket) {
        setInt(R.string.preference_key_restbase_ticket, rbTicket);
    }

    @IntRange(from = RbSwitch.FAILED) public static int getRequestSuccessCounter(int defaultValue) {
        return getInt(R.string.preference_key_request_successes, defaultValue);
    }

    public static void setRequestSuccessCounter(@IntRange(from = RbSwitch.FAILED) int successes) {
        setInt(R.string.preference_key_request_successes, successes);
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
                "%1$s://%2$s/api/rest_v1/");
    }

    @NonNull
    public static Uri getMediaWikiBaseUri() {
        return Uri.parse(defaultIfBlank(getString(R.string.preference_key_mediawiki_base_uri, null),
                Constants.WIKIPEDIA_URL));
    }

    public static boolean getMediaWikiBaseUriSupportsLangCode() {
        return getBoolean(R.string.preference_key_mediawiki_base_uri_supports_lang_code, true);
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

    public static boolean isShowZeroInterstitialEnabled() {
        return getBoolean(R.string.preference_key_zero_interstitial, true);
    }

    public static int zeroConfigHashCode() {
        return getInt(R.string.preference_key_zero_config_hash, 0);
    }

    public static void zeroConfigHashCode(int hashCode) {
        setInt(R.string.preference_key_zero_config_hash, hashCode);
    }

    public static boolean isSelectTextTutorialEnabled() {
        return getBoolean(R.string.preference_key_select_text_tutorial_enabled, true);
    }

    public static void setSelectTextTutorialEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_select_text_tutorial_enabled, enabled);
    }

    public static boolean isShareTutorialEnabled() {
        return getBoolean(R.string.preference_key_share_tutorial_enabled, true);
    }

    public static void setShareTutorialEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_share_tutorial_enabled, enabled);
    }

    public static boolean isReadingListTutorialEnabled() {
        return getBoolean(R.string.preference_key_reading_list_tutorial_enabled, true);
    }

    public static void setReadingListTutorialEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_reading_list_tutorial_enabled, enabled);
    }

    public static boolean isTocTutorialEnabled() {
        return getBoolean(R.string.preference_key_toc_tutorial_enabled, true);
    }

    public static void setTocTutorialEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_toc_tutorial_enabled, enabled);
    }

    public static boolean isZeroTutorialEnabled() {
        return getBoolean(R.string.preference_key_zero_tutorial_enabled, true);
    }

    public static void setZeroTutorialEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_zero_tutorial_enabled, enabled);
    }

    public static boolean isImageDownloadEnabled() {
        return getBoolean(R.string.preference_key_show_images, true);
    }

    public static String getCookiesForDomainKey(@NonNull String domain) {
        return getKey(R.string.preference_key_cookies_for_domain_format, domain);
    }

    private static String getLastRunTimeKey(@NonNull String task) {
        return getKey(R.string.preference_key_last_run_time_format, task);
    }

    public static boolean isLinkPreviewEnabled() {
        return getBoolean(R.string.preference_key_show_link_previews, true);
    }

    public static int getReadingListSortMode(int defaultValue) {
        return getInt(R.string.preference_key_reading_list_sort_mode, defaultValue);
    }

    public static void setReadingListSortMode(int sortMode) {
        setInt(R.string.preference_key_reading_list_sort_mode, sortMode);
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

    public static long getLastDescriptionEditTime() {
        return getLong(R.string.preference_key_last_description_edit_time, 0);
    }

    public static void setLastDescriptionEditTime(long time) {
        setLong(R.string.preference_key_last_description_edit_time, time);
    }

    public static long getReadingListSyncRev() {
        return getLong(R.string.preference_key_reading_list_sync_rev, 0);
    }

    public static void setReadingListSyncRev(long rev) {
        setLong(R.string.preference_key_reading_list_sync_rev, rev);
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

    public static void setReadingListsCurrentUser(@Nullable String userName) {
        setString(R.string.preference_key_reading_lists_current_user_hash,
                TextUtils.isEmpty(userName) ? "" : StringUtil.md5string(userName));
    }

    public static boolean hasReadingListsCurrentUser() {
        return !TextUtils.isEmpty(getString(R.string.preference_key_reading_lists_current_user_hash, ""));
    }

    public static boolean isReadingListsCurrentUser(@Nullable String userName) {
        return !TextUtils.isEmpty(userName)
                && getString(R.string.preference_key_reading_lists_current_user_hash, "")
                .equals(StringUtil.md5string(userName));
    }

    public static List<Compilation> getCompilationCache() {
        List<Compilation> compilations = new ArrayList<>();
        return contains(R.string.preference_key_compilation_cache)
                ? GsonUnmarshaller.unmarshal(new TypeToken<List<Compilation>>() { },
                getString(R.string.preference_key_compilation_cache, null)) : compilations;
    }

    public static void setCompilationCache(@NonNull List<Compilation> compilations) {
        setString(R.string.preference_key_compilation_cache, GsonMarshaller.marshal(compilations));
    }

    public static void setOfflineTutorialCardEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_offline_onboarding_card_enabled, enabled);
    }

    public static boolean isOfflineTutorialEnabled() {
        return getBoolean(R.string.preference_key_offline_onboarding_enabled, false);
    }

    public static void setOfflineTutorialEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_offline_onboarding_enabled, enabled);
    }

    public static boolean shouldDimDarkModeImages() {
        return getBoolean(R.string.preference_key_dim_dark_mode_images, true);
    }

    public static void setDimDarkModeImages(boolean enabled) {
        setBoolean(R.string.preference_key_dim_dark_mode_images, enabled);
    }

    public static boolean suppressNotificationPolling() {
        return getBoolean(R.string.preference_key_suppress_notification_polling, false);
    }

    public static boolean preferOfflineContent() {
        return getBoolean(R.string.preference_key_prefer_offline_content, false);
    }

    public static boolean offlineLibraryEnabled() {
        return getBoolean(R.string.preference_key_enable_offline_library, false);
    }

    @NonNull public static List<Boolean> getFeedCardsEnabled() {
        if (!contains(R.string.preference_key_feed_cards_enabled)) {
            return Collections.emptyList();
        }
        //noinspection unchecked
        List<Boolean> enabledList = GsonUnmarshaller.unmarshal(new TypeToken<ArrayList<Boolean>>(){},
                getString(R.string.preference_key_feed_cards_enabled, null));
        return enabledList != null ? enabledList : Collections.emptyList();
    }

    public static void setFeedCardsEnabled(@NonNull List<Boolean> enabledList) {
        setString(R.string.preference_key_feed_cards_enabled, GsonMarshaller.marshal(enabledList));
    }

    @NonNull public static List<Integer> getFeedCardsOrder() {
        if (!contains(R.string.preference_key_feed_cards_order)) {
            return Collections.emptyList();
        }
        //noinspection unchecked
        List<Integer> orderList = GsonUnmarshaller.unmarshal(new TypeToken<ArrayList<Integer>>(){},
                getString(R.string.preference_key_feed_cards_order, null));
        return orderList != null ? orderList : Collections.emptyList();
    }

    public static void setFeedCardsOrder(@NonNull List<Integer> orderList) {
        setString(R.string.preference_key_feed_cards_order, GsonMarshaller.marshal(orderList));
    }

    public static void resetFeedCustomizations() {
        remove(R.string.preference_key_feed_hidden_cards);
        remove(R.string.preference_key_feed_cards_enabled);
        remove(R.string.preference_key_feed_cards_order);
    }

    public static void setFeedCustomizeTutorialCardEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_feed_customize_onboarding_card_enabled, enabled);
    }

    public static String getReadingListsLastSyncTime() {
        return getString(R.string.preference_key_reading_lists_last_sync_time, "");
    }

    public static void setReadingListsLastSyncTime(@Nullable String timeStr) {
        setString(R.string.preference_key_reading_lists_last_sync_time, timeStr);
    }

    @NonNull public static Set<Long> getReadingListsDeletedIds() {
        Set<Long> set = new HashSet<>();
        if (!contains(R.string.preference_key_reading_lists_deleted_ids)) {
            return set;
        }
        //noinspection unchecked
        Set<Long> tempSet = GsonUnmarshaller.unmarshal(new TypeToken<Set<Long>>(){},
                getString(R.string.preference_key_reading_lists_deleted_ids, null));
        if (tempSet != null) {
            set.addAll(tempSet);
        }
        return set;
    }

    public static void addReadingListsDeletedIds(@NonNull Set<Long> set) {
        final int maxStoredIds = 256;
        Set<Long> currentSet = getReadingListsDeletedIds();
        currentSet.addAll(set);
        setReadingListsDeletedIds(currentSet.size() < maxStoredIds ? currentSet : set);
    }

    public static void setReadingListsDeletedIds(@NonNull Set<Long> set) {
        setString(R.string.preference_key_reading_lists_deleted_ids, GsonMarshaller.marshal(set));
    }

    @NonNull public static Set<String> getReadingListPagesDeletedIds() {
        Set<String> set = new HashSet<>();
        if (!contains(R.string.preference_key_reading_lists_deleted_ids)) {
            return set;
        }
        //noinspection unchecked
        Set<String> tempSet = GsonUnmarshaller.unmarshal(new TypeToken<Set<String>>(){},
                getString(R.string.preference_key_reading_list_pages_deleted_ids, null));
        if (tempSet != null) {
            set.addAll(tempSet);
        }
        return set;
    }

    public static void addReadingListPagesDeletedIds(@NonNull Set<String> set) {
        final int maxStoredIds = 256;
        Set<String> currentSet = getReadingListPagesDeletedIds();
        currentSet.addAll(set);
        setReadingListPagesDeletedIds(currentSet.size() < maxStoredIds ? currentSet : set);
    }

    public static void setReadingListPagesDeletedIds(@NonNull Set<String> set) {
        setString(R.string.preference_key_reading_list_pages_deleted_ids, GsonMarshaller.marshal(set));
    }

    public static boolean shouldShowReadingListSyncEnablePrompt() {
        return getBoolean(R.string.preference_key_show_reading_lists_sync_prompt, true);
    }

    public static void shouldShowReadingListSyncEnablePrompt(boolean enabled) {
        setBoolean(R.string.preference_key_show_reading_lists_sync_prompt, enabled);
    }

    public static boolean shouldShowReadingListSyncMergePrompt() {
        return getBoolean(R.string.preference_key_show_reading_lists_merge_prompt, true);
    }

    public static void shouldShowReadingListSyncMergePrompt(boolean enabled) {
        setBoolean(R.string.preference_key_show_reading_lists_merge_prompt, enabled);
    }

    public static boolean isReadingListsFirstTimeSync() {
        return getBoolean(R.string.preference_key_reading_lists_first_time_sync, true);
    }

    public static void setReadingListsFirstTimeSync(boolean value) {
        setBoolean(R.string.preference_key_reading_lists_first_time_sync, value);
    }

    private Prefs() { }
}
