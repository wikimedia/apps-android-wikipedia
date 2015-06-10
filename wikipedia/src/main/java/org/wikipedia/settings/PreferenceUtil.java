package org.wikipedia.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;

public final class PreferenceUtil {
    public static SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Nullable
    public static String getAppLanguageCode() {
        return getString(PrefKeys.getContentLanguageKey(), null);
    }

    public static void setAppLanguageCode(String code) {
        setString(PrefKeys.getContentLanguageKey(), code);
    }

    @Nullable
    public static String getMruLanguageCodes() {
        return getString(PrefKeys.getLanguageMru(), null);
    }

    public static void setMruLanguageCodes(String csv) {
        setString(PrefKeys.getLanguageMru(), csv);
    }

    private static String getString(String key, String defaultValue) {
        return getPreferences().getString(key, defaultValue);
    }

    private static void setString(String key, String value) {
        getPreferences().edit().putString(key, value).apply();
    }

    private static Context getContext() {
        return WikipediaApp.getInstance();
    }

    private PreferenceUtil() {
    }
}