package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

public class GalleryFunnel extends TimedFunnel {
    public static final int SOURCE_LEAD_IMAGE = 0;
    public static final int SOURCE_NON_LEAD_IMAGE = 1;
    public static final int SOURCE_LINK_PREVIEW = 2;

    private static final String SCHEMA_NAME = "MobileWikiAppMediaGallery";
    private static final int REV_ID = 12588701;

    private final int source;

    public GalleryFunnel(WikipediaApp app, Site site, int source) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_100, site);
        this.source = source;
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "source", source);
        return super.preprocessData(eventData);
    }

    @NonNull
    @Override
    protected String getSessionTokenField() {
        return "gallerySessionToken";
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
