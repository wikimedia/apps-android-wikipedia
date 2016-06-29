package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.search.SearchArticlesFragment;

public class SearchFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppSearch";
    private static final int REVISION = 15729321;
    private SearchArticlesFragment.InvokeSource source;

    public SearchFunnel(WikipediaApp app, SearchArticlesFragment.InvokeSource source) {
        super(app, SCHEMA_NAME, REVISION, Funnel.SAMPLE_LOG_100);
        this.source = source;
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

    public void searchClick(int position) {
        log(
                "action", "click",
                "position", position
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

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "source", source.code());
        return super.preprocessData(eventData);
    }

    @NonNull
    @Override
    protected String getSessionTokenField() {
        return "searchSessionToken";
    }
}
