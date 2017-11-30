package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.FeedContentType;

import java.util.List;

public class FeedConfigureFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppFeedConfigure";
    private static final int REV_ID = 17490595;

    private final int source;

    public FeedConfigureFunnel(WikipediaApp app, WikiSite wiki, int source) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL, wiki);
        this.source = source;
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    public void done(List<FeedContentType> orderedContentTypes) {
        StringBuilder enabledStr = new StringBuilder();
        StringBuilder orderStr = new StringBuilder();
        for (FeedContentType type : FeedContentType.values()) {
            if (enabledStr.length() > 0) {
                enabledStr.append(",");
            }
            enabledStr.append(type.isEnabled() ? 1 : 0);
        }
        for (FeedContentType type : orderedContentTypes) {
            if (orderStr.length() > 0) {
                orderStr.append(",");
            }
            orderStr.append(type.code());
        }
        log(
                "source", source,
                "enabledList", enabledStr.toString(),
                "orderList", orderStr.toString()
        );
    }
}
