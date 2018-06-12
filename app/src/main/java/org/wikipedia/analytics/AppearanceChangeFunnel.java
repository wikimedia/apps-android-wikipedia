package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.theme.Theme;

public class AppearanceChangeFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppAppearanceSettings";
    private static final int REV_ID = 18113727;

    public AppearanceChangeFunnel(WikipediaApp app, WikiSite wiki) {
        super(app, SCHEMA_NAME, REV_ID, wiki);
    }

    public void logFontSizeChange(float currentFontSize, float newFontSize) {
        log(
                "action", "fontSizeChange",
                "current_value", String.valueOf(currentFontSize),
                "new_value", String.valueOf(newFontSize)
        );
    }

    public void logThemeChange(Theme currentTheme, Theme newTheme) {
        log(
                "action", "themeChange",
                "current_value", currentTheme.getFunnelName(),
                "new_value", newTheme.getFunnelName()
        );
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }
}
