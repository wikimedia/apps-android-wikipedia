package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.page.PageTitle;
import org.wikipedia.WikipediaApp;

public class LinkPreviewFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppLinkPreview";
    private static final int REV_ID = 12143205;
    private static final int DEFAULT_SAMPLE_RATE = 100;

    private final PageTitle title;
    private final int version;

    public LinkPreviewFunnel(WikipediaApp app, PageTitle title) {
        super(app, SCHEMA_NAME, REV_ID);
        this.title = title;
        version = app.getLinkPreviewVersion();
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "version", version);
        return super.preprocessData(eventData);
    }

    @NonNull
    @Override
    protected String getSessionTokenField() {
        return "previewSessionToken";
    }

    protected void log(Object... params) {
        // get our sampling rate from remote config
        int sampleRate = getApp().getRemoteConfig().getConfig()
                .optInt("linkPreviewLogSampleRate", getApp().isProdRelease() ? DEFAULT_SAMPLE_RATE : 1);
        super.log(title.getSite(), sampleRate, params);
    }

    public void logLinkClick() {
        log(
                "action", "linkclick"
        );
    }

    public void logNavigate() {
        log(
                "action", "navigate"
        );
    }

    public void logCancel() {
        log(
                "action", "cancel"
        );
    }
}
