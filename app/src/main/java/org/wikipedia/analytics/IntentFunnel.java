package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;

public class IntentFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppIntents";
    private static final int REV_ID = 15237384;

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
