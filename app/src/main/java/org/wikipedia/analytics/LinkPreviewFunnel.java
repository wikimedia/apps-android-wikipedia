package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.page.PageTitle;
import org.wikipedia.WikipediaApp;

public class LinkPreviewFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppLinkPreview";
    private static final int REV_ID = 12143205;
    private static final int LINK_PREVIEW_VERSION = 3;

    private final PageTitle title;

    public LinkPreviewFunnel(WikipediaApp app, PageTitle title) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_100);
        this.title = title;
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "version", LINK_PREVIEW_VERSION);
        return super.preprocessData(eventData);
    }

    @NonNull
    @Override
    protected String getSessionTokenField() {
        return "previewSessionToken";
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
