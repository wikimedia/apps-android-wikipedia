package org.wikipedia.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.wikipedia.WikipediaApp;

import java.util.Collections;
import java.util.Set;

/** Shared preferences input / output utility providing set* functionality that writes to SP on the
 * client's behalf, IO without client supplied {@link Context}, and wrappers for using string
 * resources as keys, and unifies SP access. */
public final class PrefsIoUtil {
    @Nullable
    public static String getString(@StringRes int id, @Nullable String defaultValue) {
        return getString(getKey(id), defaultValue);
    }

    public static void setString(@StringRes int id, @Nullable String value) {
        setString(getKey(id), value);
    }

    @Nullable
    public static Set<String> getStringSet(@StringRes int id, @Nullable Set<String> defaultValue) {
        return getStringSet(getKey(id), defaultValue);
    }

    public static void setStringSet(@StringRes int id, @Nullable Set<String> value) {
        setStringSet(getKey(id), value);
    }

    public static long getLong(@StringRes int id, long defaultValue) {
        return getLong(getKey(id), defaultValue);
    }

    public static void setLong(@StringRes int id, long value) {
        setLong(getKey(id), value);
    }

    public static int getInt(@StringRes int id, int defaultValue) {
        return getInt(getKey(id), defaultValue);
    }

    public static void setInt(@StringRes int id, int value) {
        setInt(getKey(id), value);
    }

    public static boolean getBoolean(@StringRes int id, boolean defaultValue) {
        return getBoolean(getKey(id), defaultValue);
    }

    public static void setBoolean(@StringRes int id, boolean value) {
        setBoolean(getKey(id), value);
    }

    @Nullable
    public static String getString(String key, @Nullable String defaultValue) {
        return getPreferences().getString(key, defaultValue);
    }

    public static void setString(String key, @Nullable String value) {
        edit().putString(key, value).apply();
    }

    @Nullable
    public static Set<String> getStringSet(String key, @Nullable Set<String> defaultValue) {
        Set<String> set = getPreferences().getStringSet(key, defaultValue);
        return set == null ? null : Collections.unmodifiableSet(set);
    }

    public static void setStringSet(String key, @Nullable Set<String> value) {
        edit().putStringSet(key, value).apply();
    }

    public static long getLong(String key, long defaultValue) {
        return getPreferences().getLong(key, defaultValue);
    }

    public static void setLong(String key, long value) {
        edit().putLong(key, value).apply();
    }

    public static int getInt(String key, int defaultValue) {
        return getPreferences().getInt(key, defaultValue);
    }

    public static void setInt(String key, int value) {
        edit().putInt(key, value).apply();
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return getPreferences().getBoolean(key, defaultValue);
    }

    public static void setBoolean(String key, boolean value) {
        edit().putBoolean(key, value).apply();
    }

    public static void remove(@StringRes int id) {
        remove(getKey(id));
    }

    public static void remove(String key) {
        edit().remove(key).apply();
    }

    public static boolean contains(@StringRes int id) {
        return getPreferences().contains(getKey(id));
    }

    public static boolean contains(String key) {
        return getPreferences().contains(key);
    }

    /** @return Key String resource from preference_keys.xml. */
    @NonNull
    public static String getKey(@StringRes int id, Object... formatArgs) {
        return getResources().getString(id, formatArgs);
    }

    @NonNull
    private static SharedPreferences.Editor edit() {
        return getPreferences().edit();
    }

    @NonNull
    private static SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @NonNull
    private static Resources getResources() {
        return getContext().getResources();
    }

    @NonNull
    private static Context getContext() {
        return WikipediaApp.getInstance();
    }

    private PrefsIoUtil() { }
}
