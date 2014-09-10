package org.wikipedia.settings;

import android.content.res.Resources;
import org.wikipedia.R;

/** Keys in SharedPreference file used by this App. */
public final class PrefKeys {
    private static String PREFERENCE_CONTENT_LANGUAGE;
    private static String PREFERENCE_COOKIE_DOMAINS;
    private static String PREFERENCE_COOKIES_FOR_DOMAINS;
    private static String PREFERENCE_EDITTOKEN_WIKIS;
    private static String PREFERENCE_EDITTOKEN_FOR_WIKI;
    private static String PREFERENCE_ZERO_INTERSTITIAL;
    private static String PREFERENCE_REMOTE_CONFIG;
    private static String PREFERENCE_EVENTLOGGING_ENABLED;
    private static String PREFERENCE_STYLES_LAST_UPDATED;
    private static String PREFERENCE_READING_APP_INSTALL_ID;
    private static String PREFERENCE_ONBOARD;
    private static String PREFERENCE_TEXT_SIZE_MULTIPLIER;
    private static String PREFERENCE_COLOR_THEME;
    private static String PREFERENCE_CHANNEL;
    private static String PREFERENCE_LANGUAGE_MRU;
    private static String PREFERENCE_KNOW_TOC_DRAWER;

    private PrefKeys() { }

    public static void initPreferenceKeys(Resources resources) {
        PREFERENCE_CONTENT_LANGUAGE = resources.getString(R.string.preference_key_language);
        PREFERENCE_COOKIE_DOMAINS = resources.getString(R.string.preference_cookie_domains);
        PREFERENCE_COOKIES_FOR_DOMAINS = resources.getString(R.string.preference_cookies_for_domain);
        PREFERENCE_EDITTOKEN_WIKIS = resources.getString(R.string.preference_edittoken_wikis);
        PREFERENCE_EDITTOKEN_FOR_WIKI = resources.getString(R.string.preference_edittoken_for_wiki);
        PREFERENCE_ZERO_INTERSTITIAL = resources.getString(R.string.preference_key_zero_interstitial);
        PREFERENCE_REMOTE_CONFIG = resources.getString(R.string.preference_key_remote_config);
        PREFERENCE_EVENTLOGGING_ENABLED = resources.getString(R.string.preference_key_eventlogging_opt_in);
        PREFERENCE_STYLES_LAST_UPDATED = resources.getString(R.string.preference_key_styles_last_updated);
        PREFERENCE_READING_APP_INSTALL_ID = resources.getString(R.string.preference_reading_app_install_id);
        PREFERENCE_ONBOARD = resources.getString(R.string.preference_onboard);
        PREFERENCE_TEXT_SIZE_MULTIPLIER = resources.getString(R.string.preference_text_size_multiplier);
        PREFERENCE_COLOR_THEME = resources.getString(R.string.preference_color_theme);
        PREFERENCE_CHANNEL = resources.getString(R.string.preference_channel);
        PREFERENCE_LANGUAGE_MRU = resources.getString(R.string.preference_language_mru);
        PREFERENCE_KNOW_TOC_DRAWER = resources.getString(R.string.preference_know_toc_drawer);
    }

    public static String getContentLanguageKey() {
        return PREFERENCE_CONTENT_LANGUAGE;
    }

    public static String getCookieDomainsKey() {
        return PREFERENCE_COOKIE_DOMAINS;
    }

    public static String getCookiesForDomain() {
        return PREFERENCE_COOKIES_FOR_DOMAINS;
    }

    public static String getEditTokenWikis() {
        return PREFERENCE_EDITTOKEN_WIKIS;
    }

    public static String getEditTokenForWiki() {
        return PREFERENCE_EDITTOKEN_FOR_WIKI;
    }

    public static String getZeroInterstitial() {
        return PREFERENCE_ZERO_INTERSTITIAL;
    }

    public static String getRemoteConfig() {
        return PREFERENCE_REMOTE_CONFIG;
    }

    public static String getEventLoggingEnabled() {
        return PREFERENCE_EVENTLOGGING_ENABLED;
    }

    public static String getStylesLastUpdated() {
        return PREFERENCE_STYLES_LAST_UPDATED;
    }

    public static String getReadingAppInstallId() {
        return PREFERENCE_READING_APP_INSTALL_ID;
    }

    public static String getOnboard() {
        return PREFERENCE_ONBOARD;
    }

    public static String getTextSizeMultiplier() {
        return PREFERENCE_TEXT_SIZE_MULTIPLIER;
    }

    public static String getColorTheme() {
        return PREFERENCE_COLOR_THEME;
    }

    public static String getChannel() {
        return PREFERENCE_CHANNEL;
    }

    public static String getLanguageMru() {
        return PREFERENCE_LANGUAGE_MRU;
    }

    public static String getKnowTocDrawer() {
        return PREFERENCE_KNOW_TOC_DRAWER;
    }
}
