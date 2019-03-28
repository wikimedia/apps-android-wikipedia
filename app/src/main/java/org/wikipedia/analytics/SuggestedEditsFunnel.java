package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.wikipedia.WikipediaApp;

public class SuggestedEditsFunnel extends TimedFunnel {
    private static final String SCHEMA_NAME = "MobileWikiAppSuggestedEdits";
    private static final int REV_ID = 18949003;

    private static final String SUGGESTED_EDITS_UI_VERSION = "1.0";
    private static final String SUGGESTED_EDITS_API_VERSION = "1.0";

    public static final String SUGGESTED_EDITS_ADD_COMMENT = "#suggestededit-add " + SUGGESTED_EDITS_UI_VERSION;
    public static final String SUGGESTED_EDITS_TRANSLATE_COMMENT = "#suggestededit-translate " + SUGGESTED_EDITS_UI_VERSION;

    private String parentSessionToken;
    private int helpOpenedCount = 0;

    public SuggestedEditsFunnel(WikipediaApp app, String parentSessionToken) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL);
        this.parentSessionToken = parentSessionToken;
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) {
        preprocessData(eventData, "session_token", parentSessionToken);
    }

    public void helpOpened() {
        helpOpenedCount++;
    }

    @Override
    protected JSONObject preprocessData(@NonNull JSONObject eventData) {
        preprocessData(eventData, "ui_version", SUGGESTED_EDITS_UI_VERSION);
        preprocessData(eventData, "api_version", SUGGESTED_EDITS_API_VERSION);
        return super.preprocessData(eventData);
    }

    public void log() {
        log(
                "help_opened", helpOpenedCount
        );
    }
}
