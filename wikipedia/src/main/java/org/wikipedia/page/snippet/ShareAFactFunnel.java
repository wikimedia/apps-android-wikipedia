package org.wikipedia.page.snippet;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.Funnel;

import java.util.UUID;

public class ShareAFactFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppShareAFact";
    private static final int REV_ID = 10916168;

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

    private void logAction(String action, String text) {
        log(
                "action", action,
                "article", pageTitle,
                "pageID", pageId,
                "revID", revisionId,
                "text", text
        );
    }

    public void logHighlight() {
        logAction("highlight", null);
    }

    public void logShareIntent(String text) {
        logAction("shareintent", text);
    }
}
