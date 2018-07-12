package org.wikipedia.analytics;

import org.wikipedia.WikipediaApp;

public class AppLanguageSettingsFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppLanguageSettings";
    private static final int REV_ID = 18113720;

    public AppLanguageSettingsFunnel() {
        super(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL);
    }

    public void logLanguageSetting(String source, String initialLanguageList, String finalLanguageList, int interactionsCount, boolean searched) {
        log(
                "source", source,
                "initial", initialLanguageList,
                "final", finalLanguageList,
                "interactions", interactionsCount,
                "searched", searched
        );
    }
}
