package org.wikipedia.analytics;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

public class AppearanceChangeFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppAppearanceSettings";
    private static final int REV_ID = 10375462;

    private final String appInstallID;
    private final Site site;

    public AppearanceChangeFunnel(WikipediaApp app, Site site) {
        super(app, SCHEMA_NAME, REV_ID);

        //Retrieve this app installation's unique ID, used to record unique users of features
        appInstallID = app.getAppInstallID();

        this.site = site;
    }

    @Override
    protected JSONObject preprocessData(JSONObject eventData) {
        try {
            eventData.put("appInstallID", appInstallID);
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
