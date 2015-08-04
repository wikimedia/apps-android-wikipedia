package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

public class ToCInteractionFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppToCInteraction";
    private static final int REV_ID = 11014396;

    public ToCInteractionFunnel(WikipediaApp app, Site site) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_100, site);
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    public void logOpen() {
        log(
                "action", "open"
        );
    }

    public void logClose() {
        log(
                "action", "close"
        );
    }

    public void logClick() {
        log(
                "action", "click"
        );
    }
}
