package org.wikipedia.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.SparseArray;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.language.LanguageUtil;
import org.wikipedia.page.PageTitle;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static android.text.format.DateUtils.getRelativeTimeSpanString;
import static java.lang.System.currentTimeMillis;
import static java.util.Locale.SIMPLIFIED_CHINESE;
import static java.util.Locale.TRADITIONAL_CHINESE;
import static org.wikipedia.language.AppLanguageLookUpTable.CHINESE_LANGUAGE_CODE;
import static org.wikipedia.language.AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE;
import static org.wikipedia.language.AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE;

/**
 * A collection of localization related methods.
 *
 * Note the distinction between Article language and device language.
 * Article language is the language of the current page content.
 * Device language is the current language setting in the device system settings.
 * Those can be different.
 */
public final class L10nUtil {
    /**
     * List of wiki language codes for which the content is primarily RTL.
     *
     * Ensure that this is always sorted alphabetically.
     */
    private static final String[] RTL_LANGS = {
            "ar", "arc", "arz", "bcc", "bqi", "ckb", "dv", "fa", "glk", "he",
            "khw", "ks", "mzn", "pnb", "ps", "sd", "ug", "ur", "yi"
    };

    /**
     * Returns true if the given wiki language is to be displayed RTL.
     *
     * @param lang Wiki code for the language to check for directionality
     * @return true if it is RTL, false if LTR
     */
    public static boolean isLangRTL(String lang) {
        return Arrays.binarySearch(RTL_LANGS, lang, null) >= 0;
    }

    /**
     * Set up directionality for both UI and content elements in a webview.
     *
     * @param contentLang The Content language to use to set directionality. Wiki Language code.
     * @param uiLang The UI language to use to set directionality. Java language code.
     * @param bridge The CommunicationBridge to use to communicate with the WebView
     */
    public static void setupDirectionality(String contentLang, String uiLang, CommunicationBridge bridge) {
        JSONObject payload = new JSONObject();
        try {
            if (isLangRTL(contentLang)) {
                payload.put("contentDirection", "rtl");
            } else {
                payload.put("contentDirection", "ltr");
            }
            if (isLangRTL(LanguageUtil.languageCodeToWikiLanguageCode(uiLang))) {
                payload.put("uiDirection", "rtl");
            } else {
                payload.put("uiDirection", "ltr");
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setDirectionality", payload);
    }

    /**
     * Sets text direction (RTL / LTR) for given view based on given lang.
     *
     * Doesn't do anything on pre Android 4.2, since their RTL support is terrible.
     *
     * @param view View to set text direction of
     * @param lang Wiki code for the language based on which to set direction
     */
    public static void setConditionalTextDirection(View view, String lang) {
        view.setTextDirection(isLangRTL(lang) ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
    }

    /**
     * Sets layout direction (RTL / LTR) for given view based on given lang.
     *
     * Doesn't do anything on pre Android 4.2, since their RTL support is terrible.
     *
     * @param view View to set layout direction of
     * @param lang Wiki code for the language based on which to set direction
     */
    public static void setConditionalLayoutDirection(View view, String lang) {
        view.setLayoutDirection(isLangRTL(lang) ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
    }

    /**
     * Returns true if the translated string for the stylized WP wordmark is equivalent to the
     * English one, so that the PNG image could be used instead. We'd like to avoid bloating up our
     * APK size with extra fonts just to show the logo in the correct font, which we only use
     * rarely (Initial onboarding and ShareAFact).
     * As a compromise we use the PNG image with the correct font for the mainly used
     * languages (and also for languages that haven't translated this value). For all other
     * languages we use a font already available in Android.
     *
     * @param context any valid Context will do (even ApplicationContext)
     * @return true if the translated stylized WP logo text is the same as in English.
     */
    public static boolean canLangUseImageForWikipediaWordmark(Context context) {
        return "<big>W</big>IKIPEDI<big>A</big>".equals(context.getString(R.string.wp_stylized));
    }

    /**
     * Returns true if the device languages is set to an RTL language. Note that this includes
     * RTL_Arabic (AL).
     *
     * @return true if RTL, false if not RTL
     */
    public static boolean isDeviceRTL() {
        return isCharRTL(Locale.getDefault().getDisplayName().charAt(0));
    }

    public static boolean isCharRTL(char c) {
        final int dir = Character.getDirectionality(c);
        return dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }

    public static String getStringForArticleLanguage(PageTitle title, int resId) {
        return getStringsForLocale(new Locale(title.getWikiSite().languageCode()), new int[]{resId}).get(resId);
    }

    public static SparseArray<String> getStringsForArticleLanguage(PageTitle title, int[] resId) {
        return getStringsForLocale(new Locale(title.getWikiSite().languageCode()), resId);
    }

    /**
     * Get a string resource associated with a specific target locale.  This requires working around
     * Android's internal localization logic; as such, it isn't pretty.
     *
     * See http://stackoverflow.com/a/6380008 (submitted by WMF's own Anomie!).
     */
    private static SparseArray<String> getStringsForLocale(@NonNull Locale targetLocale,
                                                           @StringRes int[] strings) {
        Configuration config = getCurrentConfiguration();
        Locale systemLocale = ConfigurationCompat.getLocale(config);
        setDesiredLocale(config, targetLocale);
        SparseArray<String> localizedStrings = getTargetStrings(strings, config);
        config.setLocale(systemLocale);
        resetConfiguration(config);
        return localizedStrings;
    }

    private static Configuration getCurrentConfiguration() {
        return new Configuration(WikipediaApp.getInstance().getResources().getConfiguration());
    }

    private static SparseArray<String> getTargetStrings(@StringRes int[] strings, Configuration altConfig) {
        SparseArray<String> localizedStrings = new SparseArray<>();
        Resources targetResources = new Resources(WikipediaApp.getInstance().getResources().getAssets(),
                                                  WikipediaApp.getInstance().getResources().getDisplayMetrics(),
                                                  altConfig);
        for (int stringRes : strings) {
            localizedStrings.put(stringRes, targetResources.getString(stringRes));
        }
        return localizedStrings;
    }

    /**
     * Reset the system resources by initializing a new Resources object with the original configuration.
     * @param defaultConfig The original system configuration
     */
    private static void resetConfiguration(Configuration defaultConfig) {
        new Resources(WikipediaApp.getInstance().getResources().getAssets(),
                      WikipediaApp.getInstance().getResources().getDisplayMetrics(),
                      defaultConfig);
    }

    /**
     * Formats provided date relative to the current system time
     * @param date Date to format
     * @return String representing the relative time difference of the paramter from current time
     */
    public static String formatDateRelative(Date date) {
        return getRelativeTimeSpanString(date.getTime(), currentTimeMillis(), SECOND_IN_MILLIS, 0).toString();
    }

    public static void setDesiredLocale(@NonNull Configuration config, @NonNull Locale desiredLocale) {
        // when loads API in chinese variant, we can get zh-hant, zh-hans and zh
        // but if we want to display chinese correctly based on the article itself, we have to
        // detect the variant from the API responses; otherwise, we will only get english texts.
        // And this might only happen in Chinese variant

        if (desiredLocale.getLanguage().equals(TRADITIONAL_CHINESE_LANGUAGE_CODE)) {
            config.setLocale(TRADITIONAL_CHINESE);
        } else if (desiredLocale.getLanguage().equals(SIMPLIFIED_CHINESE_LANGUAGE_CODE)) {
            config.setLocale(SIMPLIFIED_CHINESE);
        } else if (desiredLocale.getLanguage().equals(CHINESE_LANGUAGE_CODE)) {
            // create a new Locale object to manage only "zh" language code based on its app language
            // code. e.g.: search "HK" article in "zh-hant" or "zh-hans" will get "zh" language code
            String appLanguageCode = WikipediaApp.getInstance().getAppLanguageCode();
            if (appLanguageCode.equals(TRADITIONAL_CHINESE_LANGUAGE_CODE)) {
                config.setLocale(TRADITIONAL_CHINESE);
            } else if (appLanguageCode.equals(SIMPLIFIED_CHINESE_LANGUAGE_CODE)) {
                config.setLocale(SIMPLIFIED_CHINESE);
            } else {
                config.setLocale(desiredLocale);
            }
        } else {
            config.setLocale(desiredLocale);
        }
    }

    // TODO: remove this if we can get correct language counts from server
    public static int getUpdatedLanguageCountIfNeeded(String getLanguageCode, int originalLanguageCount) {

        int updatedLanguageCount = originalLanguageCount;

        if (getLanguageCode.equals(CHINESE_LANGUAGE_CODE)) {
            updatedLanguageCount = updatedLanguageCount + 2; // for both Traditional and Simplified
        } else if (getLanguageCode.equals(TRADITIONAL_CHINESE_LANGUAGE_CODE) || getLanguageCode.equals(SIMPLIFIED_CHINESE_LANGUAGE_CODE)) {
            updatedLanguageCount = updatedLanguageCount + 1;
        }

        return updatedLanguageCount;
    }

    private L10nUtil() {
    }
}
