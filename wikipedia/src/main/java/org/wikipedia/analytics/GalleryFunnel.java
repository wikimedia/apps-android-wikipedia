package org.wikipedia.analytics;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

import java.util.UUID;

public class GalleryFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppMediaGallery";
    private static final int REV_ID = 12588701;
    private static final int SOURCE_LEAD_IMAGE = 0;
    private static final int SOURCE_NON_LEAD_IMAGE = 1;

    private final String gallerySessionToken;
    private final String appInstallID;
    private final Site site;
    private final int source;

    public GalleryFunnel(WikipediaApp app, Site site, boolean fromLeadImage) {
        super(app, SCHEMA_NAME, REV_ID);

        //Retrieve this app installation's unique ID, used to record unique users of features
        appInstallID = app.getAppInstallID();
        gallerySessionToken = UUID.randomUUID().toString();
        this.source = fromLeadImage ? SOURCE_LEAD_IMAGE : SOURCE_NON_LEAD_IMAGE;
        this.site = site;
    }

    @Override
    protected JSONObject preprocessData(JSONObject eventData) {
        try {
            eventData.put("appInstallID", appInstallID);
            eventData.put("gallerySessionToken", gallerySessionToken);
            eventData.put("source", source);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return super.preprocessData(eventData);
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
