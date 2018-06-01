package org.wikipedia.analytics;

import org.wikipedia.WikipediaApp;

public class AppLanguageSearchingFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppLanguageSearching";
    private static final int REV_ID = 18110978;
    private static String SESSION_TOKEN;


    public AppLanguageSearchingFunnel(String sessionToken) {
        this(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_100);
        SESSION_TOKEN = sessionToken;
    }

    private AppLanguageSearchingFunnel(WikipediaApp app, String schemaName, int revision, int sampleRate) {
        super(app, schemaName, revision, sampleRate);
    }

    public void logLanguageAdded(boolean languageAdded, String languageCode, String searchString) {
        log(
                "session_token", SESSION_TOKEN,
                "added", languageAdded,
                "language", languageCode,
                "search_string", searchString
        );
    }

    public void logNoLanguageAdded(boolean languageAdded, String searchString) {
        log(
                "session_token", SESSION_TOKEN,
                "added", languageAdded,
                "search_string", searchString
        );
    }
}
