package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;

public class ToCInteractionFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppToCInteraction";
    private static final int REV_ID = 14585319;

    private final int pageId;
    private final int numSections;

    public ToCInteractionFunnel(WikipediaApp app, WikiSite wiki, int pageId, int numSections) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_100, wiki);
        this.pageId = pageId;
        this.numSections = numSections;
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "pageID", pageId);
        preprocessData(eventData, "numSections", numSections);
        return super.preprocessData(eventData);
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    public void logOpen() {
        resetDuration();
        log(
                "action", "open"
        );
    }

    public void logClose() {
        log(
                "action", "close"
        );
    }

    public void logClick(int sectionIndex, String sectionName) {
        log(
                "action", "click",
                "sectionIndex", sectionIndex,
                "sectionName", sectionName
        );
    }
}
