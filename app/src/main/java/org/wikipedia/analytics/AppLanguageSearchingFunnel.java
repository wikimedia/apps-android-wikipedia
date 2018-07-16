package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;

public class AppLanguageSearchingFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppLanguageSearching";
    private static final int REV_ID = 18204518;
    private final String settingsSessionToken;

    public AppLanguageSearchingFunnel(String settingsSessionToken) {
        super(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL);
        this.settingsSessionToken = settingsSessionToken;
    }

    public void logLanguageAdded(boolean languageAdded, String languageCode, String searchString) {
        log(
                "language_settings_token", settingsSessionToken,
                "added", languageAdded,
                "language", languageCode,
                "search_string", searchString
        );
    }

    public void logNoLanguageAdded(boolean languageAdded, String searchString) {
        log(
                "language_settings_token", settingsSessionToken,
                "added", languageAdded,
                "search_string", searchString
        );
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }
}
