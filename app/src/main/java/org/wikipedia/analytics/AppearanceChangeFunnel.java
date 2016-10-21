package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.theme.Theme;

public class AppearanceChangeFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppAppearanceSettings";
    private static final int REV_ID = 10375462;

    public AppearanceChangeFunnel(WikipediaApp app, WikiSite wiki) {
        super(app, SCHEMA_NAME, REV_ID, wiki);
    }

    public void logFontSizeChange(float currentFontSize, float newFontSize) {
        log(
                "action", "fontSizeChange",
                "currentValue", String.valueOf(currentFontSize),
                "newValue", String.valueOf(newFontSize)
        );
    }

    public void logThemeChange(Theme currentTheme, Theme newTheme) {
        log(
                "action", "themeChange",
                "currentValue", currentTheme.getFunnelName(),
                "newValue", newTheme.getFunnelName()
        );
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }
}
