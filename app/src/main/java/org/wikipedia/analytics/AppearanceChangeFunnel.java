package org.wikipedia.analytics;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.theme.Theme;

public class AppearanceChangeFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppAppearanceSettings";
    private static final int REV_ID = 20566858;
    private InvokeSource source;

    public AppearanceChangeFunnel(WikipediaApp app, WikiSite wiki, InvokeSource source) {
        super(app, SCHEMA_NAME, REV_ID, wiki);
        this.source = source;
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

    public void logFontThemeChange(String currentFontFamily, String newFontFamily) {
        log(
                "action", "fontThemeChange",
                "current_value", currentFontFamily,
                "new_value", newFontFamily
        );
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "invoke_source", source.ordinal());
        return super.preprocessData(eventData);
    }
}
