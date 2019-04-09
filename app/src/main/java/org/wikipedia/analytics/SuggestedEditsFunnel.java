package org.wikipedia.analytics;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;
import org.wikipedia.Constants;
import org.wikipedia.WikipediaApp;
import org.wikipedia.json.GsonUtil;

public final class SuggestedEditsFunnel extends TimedFunnel {
    private static SuggestedEditsFunnel INSTANCE;

    private static final String SCHEMA_NAME = "MobileWikiAppSuggestedEdits";
    private static final int REV_ID = 18949003;

    private static final String SUGGESTED_EDITS_UI_VERSION = "1.0";
    private static final String SUGGESTED_EDITS_API_VERSION = "1.0";

    public static final String SUGGESTED_EDITS_ADD_COMMENT = "#suggestededit-add " + SUGGESTED_EDITS_UI_VERSION;
    public static final String SUGGESTED_EDITS_TRANSLATE_COMMENT = "#suggestededit-translate " + SUGGESTED_EDITS_UI_VERSION;

    private Constants.InvokeSource invokeSource;
    private String parentSessionToken;
    private int helpOpenedCount = 0;
    private int contributionsOpenedCount = 0;
    private SuggestedEditStatsCollection statsCollection = new SuggestedEditStatsCollection();

    private SuggestedEditsFunnel(WikipediaApp app, Constants.InvokeSource invokeSource) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL);
        this.invokeSource = invokeSource;
        this.parentSessionToken = app.getSessionFunnel().getSessionToken();
    }

    public static SuggestedEditsFunnel get(Constants.InvokeSource invokeSource) {
        if (INSTANCE == null) {
            INSTANCE = new SuggestedEditsFunnel(WikipediaApp.getInstance(), invokeSource);
        }
        return INSTANCE;
    }

    public static SuggestedEditsFunnel get() {
        return get(Constants.InvokeSource.NAV_MENU);
    }

    public static void reset() {
        INSTANCE = null;
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) {
        preprocessData(eventData, "session_token", parentSessionToken);
    }

    public void impression(Constants.InvokeSource source) {
        if (source == Constants.InvokeSource.EDIT_FEED_TITLE_DESC) {
            statsCollection.addDescriptionStats.impressions++;
        } else if (source == Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC) {
            statsCollection.translateDescriptionStats.impressions++;
        }
    }

    public void click(Constants.InvokeSource source) {
        if (source == Constants.InvokeSource.EDIT_FEED_TITLE_DESC) {
            statsCollection.addDescriptionStats.clicks++;
        } else if (source == Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC) {
            statsCollection.translateDescriptionStats.clicks++;
        }
    }

    public void cancel(Constants.InvokeSource source) {
        if (source == Constants.InvokeSource.EDIT_FEED_TITLE_DESC) {
            statsCollection.addDescriptionStats.cancels++;
        } else if (source == Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC) {
            statsCollection.translateDescriptionStats.cancels++;
        }
    }

    public void success(Constants.InvokeSource source) {
        if (source == Constants.InvokeSource.EDIT_FEED_TITLE_DESC) {
            statsCollection.addDescriptionStats.successes++;
        } else if (source == Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC) {
            statsCollection.translateDescriptionStats.successes++;
        }
    }

    public void failure(Constants.InvokeSource source) {
        if (source == Constants.InvokeSource.EDIT_FEED_TITLE_DESC) {
            statsCollection.addDescriptionStats.failures++;
        } else if (source == Constants.InvokeSource.EDIT_FEED_TRANSLATE_TITLE_DESC) {
            statsCollection.translateDescriptionStats.failures++;
        }
    }

    public void helpOpened() {
        helpOpenedCount++;
    }

    public void contributionsOpened() {
        contributionsOpenedCount++;
    }

    public void log() {
        log(
                "edit_tasks", GsonUtil.getDefaultGson().toJson(statsCollection),
                "help_opened", helpOpenedCount,
                "scorecard_opened", contributionsOpenedCount,
                "source", (invokeSource == Constants.InvokeSource.ONBOARDING_DIALOG ? "dialog"
                        : invokeSource == Constants.InvokeSource.NOTIFICATION ? "notification" : "menu")
        );
    }

    private static class SuggestedEditStatsCollection {
        @SerializedName("add-description") private SuggestedEditStats addDescriptionStats = new SuggestedEditStats();
        @SerializedName("translate-description") private SuggestedEditStats translateDescriptionStats = new SuggestedEditStats();
    }

    @SuppressWarnings("unused")
    private static class SuggestedEditStats {
        private int impressions;
        private int clicks;
        private int cancels;
        private int successes;
        private int failures;
    }
}
