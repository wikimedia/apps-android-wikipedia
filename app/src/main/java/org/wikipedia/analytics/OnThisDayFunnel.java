package org.wikipedia.analytics;

import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.Constants;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;

public class OnThisDayFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppOnThisDay";
    private static final int REV_ID = 18118721;

    private final int source;
    private int maxScrolledPosition;

    public OnThisDayFunnel(WikipediaApp app, WikiSite wiki, Constants.InvokeSource source) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL, wiki);
        this.source = source.ordinal();
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    public void scrolledToPosition(int position) {
        if (position > maxScrolledPosition) {
            maxScrolledPosition = position;
        }
    }

    public void done(int totalOnThisDayEvents) {
        log(
                "source", source,
                "totalEvents", totalOnThisDayEvents,
                "scrolledEvents", maxScrolledPosition
        );
    }
}
