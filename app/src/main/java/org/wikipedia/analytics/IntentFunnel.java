package org.wikipedia.analytics;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;

import androidx.annotation.NonNull;

public class IntentFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppIntents";
    private static final int REV_ID = 18115555;

    public IntentFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REV_ID);
    }

    public void logSearchWidgetTap() {
        log(
                "action", "searchwidgettap"
        );
    }

    public void logFeaturedArticleWidgetTap() {
        log(
                "action", "featuredarticlewidgettap"
        );
    }

    public void logShareIntent() {
        log(
                "action", "share"
        );
    }

    public void logProcessTextIntent() {
        log(
                "action", "processtext"
        );
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }
}
