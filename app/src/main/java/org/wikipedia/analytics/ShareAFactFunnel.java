package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.Prefs;

// https://meta.wikimedia.org/wiki/Schema:MobileWikiAppShareAFact
public class ShareAFactFunnel extends Funnel {
    private static final String SCHEMA_NAME = "MobileWikiAppShareAFact";
    private static final int REV_ID = 12588711;

    /**
     * The length value of 99 is somewhat arbitrary right now. We need to restrict the
     * total length of the event data somewhat to avoid the event getting dropped.
     */
    private static final int MAX_LENGTH = 99;

    private final String pageTitle;
    private final int pageId;
    private final long revisionId;

    public ShareAFactFunnel(WikipediaApp app, PageTitle pageTitle, int pageId, long revisionId) {
        super(app, SCHEMA_NAME, REV_ID, pageTitle.getWikiSite());
        this.pageTitle = pageTitle.getDisplayText();
        this.pageId = pageId;
        this.revisionId = revisionId;
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "tutorialFeatureEnabled", true);
        preprocessData(eventData, "tutorialShown", calculateTutorialsShown());
        return super.preprocessData(eventData);
    }

    @NonNull
    @Override
    protected String getSessionTokenField() {
        return "shareSessionToken";
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
        logAction("highlight", "");
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

    private int calculateTutorialsShown() {
        return !Prefs.isShareTutorialEnabled() ? 2 : !Prefs.isSelectTextTutorialEnabled() ? 1 : 0;
    }
}
