package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.ReleaseUtil;

public class LinkPreviewFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppLinkPreview";
    private static final int REV_ID = 15730939;
    private static final int PROD_LINK_PREVIEW_VERSION = 3;
    private final int source;

    public LinkPreviewFunnel(WikipediaApp app, int source) {
        super(app, SCHEMA_NAME, REV_ID, ReleaseUtil.isProdRelease() ? Funnel.SAMPLE_LOG_100 : Funnel.SAMPLE_LOG_ALL);
        this.source = source;
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "version", PROD_LINK_PREVIEW_VERSION);
        preprocessData(eventData, "source", source);
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
                "action", Prefs.isLinkPreviewEnabled() ? "navigate" : "disabled"
        );
    }

    public void logCancel() {
        log(
                "action", "cancel"
        );
    }
}
