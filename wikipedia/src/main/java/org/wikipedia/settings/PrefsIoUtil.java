package org.wikipedia.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;

/** Shared preferences input / output utility providing set* functionality that writes to SP on the
 * client's behalf, IO without client supplied {@link Context}, and wrappers for using string
 * resources as keys, and unifies SP access. */
/*package*/ final class PrefsIoUtil {
    @Nullable
    public static String getString(int keyResourceId, @Nullable String defaultValue) {
        return getString(getKey(keyResourceId), defaultValue);
    }

    public static void setString(int keyResourceId, @Nullable String value) {
        setString(getKey(keyResourceId), value);
    }

    public static long getLong(int keyResourceId, long defaultValue) {
        return getLong(getKey(keyResourceId), defaultValue);
    }

    public static void setLong(int keyResourceId, long value) {
        setLong(getKey(keyResourceId), value);
    }

    public static int getInt(int keyResourceId, int defaultValue) {
        return getInt(getKey(keyResourceId), defaultValue);
    }

    public static void setInt(int keyResourceId, int value) {
        setInt(getKey(keyResourceId), value);
    }

    public static boolean getBoolean(int keyResourceId, boolean defaultValue) {
        return getBoolean(getKey(keyResourceId), defaultValue);
    }

    public static void setBoolean(int keyResourceId, boolean value) {
        setBoolean(getKey(keyResourceId), value);
    }

    @Nullable
    public static String getString(String key, @Nullable String defaultValue) {
        return getPreferences().getString(key, defaultValue);
    }

    public static void setString(String key, @Nullable String value) {
        edit().putString(key, value).apply();
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

    public static void remove(int keyResourceId) {
        remove(getKey(keyResourceId));
    }

    public static void remove(String key) {
        edit().remove(key).apply();
    }

    public static boolean contains(int keyResourceId) {
        return getPreferences().contains(getKey(keyResourceId));
    }

    public static boolean contains(String key) {
        return getPreferences().contains(key);
    }

    /** @return Key String resource from preference_keys.xml. */
    @NonNull
    public static String getKey(int id, Object... formatArgs) {
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