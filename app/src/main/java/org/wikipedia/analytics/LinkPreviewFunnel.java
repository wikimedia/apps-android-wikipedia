package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.page.PageTitle;
import org.wikipedia.WikipediaApp;

public class LinkPreviewFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppLinkPreview";
    private static final int REV_ID = 12143205;

    private final PageTitle title;
    private final int version;

    public LinkPreviewFunnel(WikipediaApp app, PageTitle title) {
        // TODO: increase this sample rate when ready for production
        // (we're keeping it low for now, while we gather as much engagement data as possible
        // from the beta channel)
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_10);
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
