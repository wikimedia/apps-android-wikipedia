package org.wikipedia.beta.analytics;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.WikipediaApp;

import java.util.UUID;

public class AppearanceChangeFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppAppearanceSettings";
    private static final int REV_ID = 9378399;

    private static final String APP_ID_PREF_NAME = "ANALYTICS_APP_ID_FOR_APPEARANCE";

    private final String appInstallToCInteractionID;
    private final Site site;

    public AppearanceChangeFunnel(WikipediaApp app, Site site) {
        super(app, SCHEMA_NAME, REV_ID);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
        if (prefs.contains(APP_ID_PREF_NAME)) {
            appInstallToCInteractionID = prefs.getString(APP_ID_PREF_NAME, null);
        } else {
            appInstallToCInteractionID = UUID.randomUUID().toString();
            prefs.edit().putString(APP_ID_PREF_NAME, appInstallToCInteractionID).commit();
        }

        this.site = site;
    }

    @Override
    protected JSONObject preprocessData(JSONObject eventData) {
        try {
            eventData.put("appearanceAppInstallID", appInstallToCInteractionID);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return eventData;
    }

    protected void log(Object... params) {
        super.log(site, params);
    }

    public void logFontSizeChange(float currentFontSize, float newFontSize) {
        log(
                "action", "fontSizeChange",
                "currentValue", String.valueOf(currentFontSize),
                "newValue", String.valueOf(newFontSize)
        );
    }

    private String getThemeName(int theme) {
        switch (theme) {
            case WikipediaApp.THEME_DARK:
                return "dark";
            case WikipediaApp.THEME_LIGHT:
                return "light";
            default:
                throw new RuntimeException("Unknown theme encountered!");
        }
    }

    public void logThemeChange(int currentTheme, int newTheme) {
        log(
                "action", "themeChange",
                "currentValue", getThemeName(currentTheme),
                "newValue", getThemeName(newTheme)
        );
    }

}
