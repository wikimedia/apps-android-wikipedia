package org.wikipedia.analytics;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

public class GalleryFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppMediaGallery";
    private static final int REV_ID = 10914526;

    private final String appInstallID;
    private final Site site;

    public GalleryFunnel(WikipediaApp app, Site site) {
        super(app, SCHEMA_NAME, REV_ID);

        //Retrieve this app installation's unique ID, used to record unique users of features
        appInstallID = app.getAppInstallID();

        this.site = site;
    }

    @Override
    protected JSONObject preprocessData(JSONObject eventData) {
        try {
            eventData.put("appInstallID", appInstallID);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return eventData;
    }

    protected void log(Object... params) {
        super.log(site, params);
    }

    private void logGalleryAction(String action, PageTitle currentPageTitle, String currentMediaTitle) {
        log(
                "action", action,
                "pageTitle", currentPageTitle.getDisplayText(),
                "imageTitle", currentMediaTitle
        );
    }

    public void logGalleryOpen(PageTitle currentPageTitle, String currentMediaTitle) {
        logGalleryAction("open", currentPageTitle, currentMediaTitle);
    }

    public void logGalleryClose(PageTitle currentPageTitle, String currentMediaTitle) {
        logGalleryAction("close", currentPageTitle, currentMediaTitle);
    }

    public void logGallerySwipeLeft(PageTitle currentPageTitle, String currentMediaTitle) {
        logGalleryAction("swipeLeft", currentPageTitle, currentMediaTitle);
    }

    public void logGallerySwipeRight(PageTitle currentPageTitle, String currentMediaTitle) {
        logGalleryAction("swipeRight", currentPageTitle, currentMediaTitle);
    }

    public void logGalleryShare(PageTitle currentPageTitle, String currentMediaTitle) {
        logGalleryAction("share", currentPageTitle, currentMediaTitle);
    }

    public void logGallerySave(PageTitle currentPageTitle, String currentMediaTitle) {
        logGalleryAction("save", currentPageTitle, currentMediaTitle);
    }

}
