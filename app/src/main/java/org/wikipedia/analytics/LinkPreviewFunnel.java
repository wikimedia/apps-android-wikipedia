package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.settings.Prefs;

public class LinkPreviewFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppLinkPreview";
    private static final int REV_ID = 18531254;
    private static final int PROD_LINK_PREVIEW_VERSION = 3;
    private final int source;
    private int pageId;

    public LinkPreviewFunnel(WikipediaApp app, int source) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL);
        this.source = source;
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "version", PROD_LINK_PREVIEW_VERSION);
        preprocessData(eventData, "source", source);
        preprocessData(eventData, "page_id", pageId);
        return super.preprocessData(eventData);
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public void logLinkClick() {
        log(
                "action", "linkclick"
        );
    }

    public void logNavigate() {
        log(
                "action", Prefs.isLinkPreviewEnabled() ? "navigate" : "disabled"
        );
    }

    public void logCancel() {
        log(
                "action", "cancel"
        );
    }
}
