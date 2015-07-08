package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

public class ToCInteractionFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppToCInteraction";
    private static final int REV_ID = 11014396;
    private static final int DEFAULT_SAMPLE_RATE = 100;

    public ToCInteractionFunnel(WikipediaApp app, Site site) {
        super(app, SCHEMA_NAME, REV_ID, site);
    }

    protected void log(Object... params) {
        //get our sampling rate from remote config
        int sampleRate = getApp().getRemoteConfig().getConfig()
                                 .optInt("tocLogSampleRate", DEFAULT_SAMPLE_RATE);
        super.log(sampleRate, params);
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
