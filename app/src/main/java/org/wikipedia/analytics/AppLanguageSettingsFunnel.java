package org.wikipedia.analytics;

import org.wikipedia.WikipediaApp;

public class AppLanguageSettingsFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppLanguageSettings";
    private static final int REV_ID = 18110934;
    private static String SESSION_TOKEN;


    public AppLanguageSettingsFunnel(String sessionToken) {
        this(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_100);
        SESSION_TOKEN = sessionToken;
    }

    private AppLanguageSettingsFunnel(WikipediaApp app, String schemaName, int revision, int sampleRate) {
        super(app, schemaName, revision, sampleRate);
    }

    public void logLanguageSetting(String source, String initialLanguageList, String finalLanguageList, int interactionsCount, boolean searched) {
        log(
                "source", source,
                "initial", initialLanguageList,
                "final", finalLanguageList,
                "interactions", interactionsCount,
                "searched", searched,
                "session_token", SESSION_TOKEN
        );
    }

}
