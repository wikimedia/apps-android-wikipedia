package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;

public class SearchFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppSearch";
    private static final int REVISION = 10641988;

    public SearchFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REVISION, Funnel.SAMPLE_LOG_100);
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
