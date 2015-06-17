package org.wikipedia.settings;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;

import static org.wikipedia.settings.PrefsIoUtil.contains;
import static org.wikipedia.settings.PrefsIoUtil.getBoolean;
import static org.wikipedia.settings.PrefsIoUtil.getInt;
import static org.wikipedia.settings.PrefsIoUtil.getKey;
import static org.wikipedia.settings.PrefsIoUtil.getString;
import static org.wikipedia.settings.PrefsIoUtil.remove;
import static org.wikipedia.settings.PrefsIoUtil.setBoolean;
import static org.wikipedia.settings.PrefsIoUtil.setInt;
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

    public static String getAppLanguageCodeKey() {
        return getKey(R.string.preference_key_language);
    }

    public static int getColorThemeResourceId() {
        return getInt(R.string.preference_key_color_theme, R.style.Theme_WikiLight);
    }

    public static void setColorThemeResourceId(int theme) {
        // TODO: Is this safe? Are resource IDs guaranteed constant across app and tool versions?
        setInt(R.string.preference_key_color_theme, theme);
    }

    @NonNull
    public static String getCookieDomains() {
        return getString(R.string.preference_key_cookie_domains, "");
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

    @NonNull
    public static String getEditTokenWikis() {
        return getString(R.string.preference_key_edittoken_wikis, "");
    }

    public static void setEditTokenWikis(@Nullable String wikis) {
        setString(R.string.preference_key_edittoken_wikis, wikis);
    }

    @Nullable
    public static String getEditTokenForWiki(@NonNull String wiki) {
        return getString(getEditTokenForWikiKey(wiki), null);
    }

    public static void setEditTokenForWiki(@NonNull String wiki, @Nullable String token) {
        setString(getEditTokenForWikiKey(wiki), token);
    }

    public static void removeEditTokenForWiki(@NonNull String wiki) {
        remove(getEditTokenForWikiKey(wiki));
    }

    public static int getFeatureFlagId() {
        return getInt(R.string.preference_key_feature_flag_id, 0);
    }

    public static void setFeatureFlagId(int id) {
        setInt(R.string.preference_key_feature_flag_id, id);
    }

    public static boolean hasFeatureFlagId() {
        return contains(R.string.preference_key_feature_flag_id);
    }

    @Nullable
    public static String getLoginUsername() {
        return getString(R.string.preference_key_login_username, null);
    }

    public static void setLoginUsername(@Nullable String username) {
        setString(R.string.preference_key_login_username, username);
    }

    public static boolean hasLoginUsername() {
        return contains(R.string.preference_key_login_username);
    }

    public static void removeLoginUsername() {
        remove(R.string.preference_key_login_username);
    }

    @Nullable
    public static String getLoginPassword() {
        return getString(R.string.preference_key_login_password, null);
    }

    public static void setLoginPassword(@Nullable String password) {
        setString(R.string.preference_key_login_password, password);
    }

    public static boolean hasLoginPassword() {
        return contains(R.string.preference_key_login_password);
    }

    public static void removeLoginPassword() {
        remove(R.string.preference_key_login_password);
    }

    public static int getLoginUserId() {
        return getInt(R.string.preference_key_login_user_id, 0);
    }

    public static void setUserId(int id) {
        setInt(R.string.preference_key_login_user_id, id);
    }

    public static void removeUserId() {
        remove(R.string.preference_key_login_user_id);
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

    public static int getTextSizeMultiplier() {
        return getInt(R.string.preference_key_text_size_multiplier, 0);
    }

    public static void setTextSizeMultiplier(int multiplier) {
        setInt(R.string.preference_key_text_size_multiplier, multiplier);
    }

    public static boolean isEventLoggingEnabled() {
        return getBoolean(R.string.preference_key_eventlogging_opt_in, true);
    }

    public static boolean isExperimentalPageLoadEnabled() {
        return getBoolean(R.string.preference_key_exp_page_load, false);
    }

    public static void setExperimentalPageLoadEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_exp_page_load, enabled);
    }

    public static boolean isLoginOnboardingEnabled() {
        return !getBoolean(R.string.preference_key_onboard, false);
    }

    public static void setLoginOnboardingEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_onboard, !enabled);
    }

    public static boolean isShowZeroInterstitialEnabled() {
        return getBoolean(R.string.preference_key_zero_interstitial, true);
    }

    public static boolean isTocTutorialEnabled() {
        return !getBoolean(R.string.preference_key_know_toc_drawer, false);
    }

    public static void setTocTutorialEnabled(boolean enabled) {
        setBoolean(R.string.preference_key_know_toc_drawer, !enabled);
    }

    public static boolean isImageDownloadEnabled() {
        return getBoolean(R.string.preference_key_show_images, true);
    }

    private static String getCookiesForDomainKey(@NonNull String domain) {
        return getKey(R.string.preference_key_cookies_for_domain_format, domain);
    }

    private static String getEditTokenForWikiKey(String wiki) {
        return getKey(R.string.preference_key_edittoken_for_wiki_format, wiki);
    }

    private Prefs() { }
}
