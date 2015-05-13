package org.wikipedia.analytics;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

import java.util.UUID;

public class ShareAFactFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppShareAFact";
    private static final int REV_ID = 11331974;

    /**
     * The length value of 99 is somewhat arbitrary right now. We need to restrict the
     * total length of the event data somewhat to avoid the event getting dropped.
     */
    private static final int MAX_LENGTH = 99;

    private final String appInstallID;
    private final String sessionToken;
    private final Site site;
    private final String pageTitle;
    private final int pageId;
    private final long revisionId;

    public ShareAFactFunnel(WikipediaApp app, PageTitle pageTitle, int pageId, long revisionId) {
        super(app, SCHEMA_NAME, REV_ID);
        this.site = pageTitle.getSite();
        this.pageTitle = pageTitle.getDisplayText();
        this.pageId = pageId;
        this.revisionId = revisionId;

        // Retrieve this app installation's unique ID, used to record unique users of features
        appInstallID = app.getAppInstallID();

        sessionToken = UUID.randomUUID().toString();
    }

    @Override
    protected JSONObject preprocessData(JSONObject eventData) {
        try {
            eventData.put("appInstallID", appInstallID);
            eventData.put("shareSessionToken", sessionToken);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return eventData;
    }

    protected void log(Object... params) {
        super.log(site, params);
    }

    private void logAction(String action, String text, ShareMode shareMode) {
        if (text != null) {
            text = text.substring(0, Math.min(MAX_LENGTH, text.length()));
        }
        log(
                "action", action,
                "article", pageTitle,
                "pageID", pageId,
                "revID", revisionId,
                "text", text,
                "sharemode", shareMode
        );
    }

    private void logAction(String action, String text) {
        logAction(action, text, null);
    }

    /** Text in the web view was highlighted. */
    public void logHighlight() {
        logAction("highlight", null);
    }

    /** The share button in the UI was tapped. */
    public void logShareTap(String text) {
        logAction("sharetap", text);
    }

    /** 'Share as image' or 'Share as text' was tapped. */
    public void logShareIntent(String text, ShareMode shareMode) {
        logAction("shareintent", text, shareMode);
    }

    /**
     * 'Share as text' and 'Share as image' was shown but cancelled and neither was chosen.
     */
    public void logAbandoned(String text) {
        logAction("abandoned", text);
    }


    public enum ShareMode {
        image,
        text
    }
}
