package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;

public class SearchFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppSearch";
    private static final int REVISION = 10641988;
    private static final int DEFAULT_SAMPLE_RATE = 100;

    public SearchFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REVISION);
    }

    protected void log(Object... params) {
        // get our sampling rate from remote config
        int sampleRate = getApp().getRemoteConfig().getConfig()
                                 .optInt("searchLogSampleRate", DEFAULT_SAMPLE_RATE);
        super.log(sampleRate, params);
    }

    public void searchStart() {
        log(
                "action", "start"
        );
    }

    public void searchCancel() {
        log(
                "action", "cancel"
        );
    }

    public void searchClick() {
        log(
                "action", "click"
        );
    }

    public void searchDidYouMean() {
        log(
                "action", "didyoumean"
        );
    }

    public void searchResults(boolean fullText, int numResults, int delayMillis) {
        log(
                "action", "results",
                "typeOfSearch", fullText ? "full" : "prefix",
                "numberOfResults", numResults,
                "timeToDisplayResults", delayMillis
        );
    }

    public void searchError(boolean fullText, int delayMillis) {
        log(
                "action", "error",
                "typeOfSearch", fullText ? "full" : "prefix",
                "timeToDisplayResults", delayMillis
        );
    }

    @NonNull
    @Override
    protected String getSessionTokenField() {
        return "searchSessionToken";
    }
}
