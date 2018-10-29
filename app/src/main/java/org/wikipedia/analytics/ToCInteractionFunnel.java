package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;

import java.util.UUID;

public class ToCInteractionFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppToCInteraction";
    private static final int REV_ID = 18389174;

    private final int pageId;
    private final int numSections;
    private String interactionToken;
    private boolean opened;

    public ToCInteractionFunnel(WikipediaApp app, WikiSite wiki, int pageId, int numSections) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_100, wiki);
        this.pageId = pageId;
        this.numSections = numSections;
        invalidate();
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "page_id", pageId);
        preprocessData(eventData, "num_sections", numSections);
        return super.preprocessData(eventData);
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) {
        preprocessData(eventData, "interaction_token", interactionToken);
    }

    private void invalidate() {
        interactionToken = UUID.randomUUID().toString();
        opened = false;
    }

    public void logOpen() {
        resetDuration();
        opened = true;
        log(
                "action", "open"
        );
    }

    public void logClose() {
        log(
                "action", "close"
        );
        invalidate();
    }

    public void logScrollStart() {
        if (!opened) {
            logOpen();
        }
        log(
                "action", "scroll_start"
        );
    }

    public void logScrollStop() {
        log(
                "action", "scroll_stop"
        );
    }

    public void logClick(int sectionIndex, String sectionName) {
        log(
                "action", "click",
                "section_index", sectionIndex,
                "section_name", sectionName
        );
    }
}
