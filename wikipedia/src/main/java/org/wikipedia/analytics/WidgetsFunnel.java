package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;

public class WidgetsFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppWidgets";
    private static final int REV_ID = 11312870;

    public WidgetsFunnel(WikipediaApp app) {
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

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }
}