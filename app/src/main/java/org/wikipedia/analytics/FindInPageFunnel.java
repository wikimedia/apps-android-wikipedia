package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;

public class FindInPageFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppFindInPage";
    private static final int REV_ID = 14586774;

    private final int pageId;
    private int pageHeight;
    private int numFindNext;
    private int numFindPrev;
    private String findText;

    public FindInPageFunnel(WikipediaApp app, WikiSite wiki, int pageId) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL, wiki);
        this.pageId = pageId;
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) { }

    public void setPageHeight(int height) {
        this.pageHeight = height;
    }

    public void addFindNext() {
        numFindNext++;
    }

    public void addFindPrev() {
        numFindPrev++;
    }

    public void setFindText(String text) {
        findText = text;
    }

    public void logDone() {
        log(
                "pageID", pageId,
                "numFindNext", numFindNext,
                "numFindPrev", numFindPrev,
                "findText", findText,
                "pageHeight", pageHeight
        );
    }
}
