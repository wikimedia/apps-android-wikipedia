package org.wikipedia.analytics;

import android.os.SystemClock;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.page.PageTitle;
import org.wikipedia.WikipediaApp;

import java.util.UUID;

public class LinkPreviewFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppLinkPreview";
    private static final int REV_ID = 12143205;
    private static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int DEFAULT_SAMPLE_RATE = 100;

    private final String previewSessionToken;
    private final PageTitle title;
    private final int version;
    private final String appInstallID;
    private double startTime;

    public LinkPreviewFunnel(WikipediaApp app, PageTitle title) {
        super(app, SCHEMA_NAME, REV_ID);
        appInstallID = app.getAppInstallID();
        previewSessionToken = UUID.randomUUID().toString();
        this.title = title;
        version = app.getLinkPreviewVersion();
        startTime = SystemClock.uptimeMillis();
    }

    @Override
    protected JSONObject preprocessData(JSONObject eventData) {
        try {
            eventData.put("previewSessionToken", previewSessionToken);
            eventData.put("appInstallID", appInstallID);
            eventData.put("version", version);
            if (eventData.getString("action").equals("navigate") || eventData.getString("action").equals("cancel")) {
                int timeSpent = (int) Math.ceil((SystemClock.uptimeMillis() - startTime) / MILLISECONDS_PER_SECOND);
                eventData.put("timeSpent", timeSpent);
            }
        } catch (JSONException e) {
            // This never happens.
            throw new RuntimeException(e);
        }
        return eventData;
    }

    protected void log(Object... params) {
        // get our sampling rate from remote config
        int sampleRate = WikipediaApp.getInstance().getRemoteConfig().getConfig()
                .optInt("linkPreviewLogSampleRate", WikipediaApp.getInstance().isProdRelease() ? DEFAULT_SAMPLE_RATE : 1);
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
