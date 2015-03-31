package org.wikipedia.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Setters and getters for share preferences.
 */
public final class Prefs {
    private Prefs() {
    }

    public static void setUsingExperimentalPageLoad(Context ctx, boolean newValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().putBoolean(PrefKeys.getExperimentalPageLoad(), newValue).apply();
    }

    public static boolean isUsingExperimentalPageLoad(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(PrefKeys.getExperimentalPageLoad(), false);
    }
}
