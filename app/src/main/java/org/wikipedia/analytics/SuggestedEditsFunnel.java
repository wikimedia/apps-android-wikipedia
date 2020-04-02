package org.wikipedia.analytics;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.WikipediaApp;
import org.wikipedia.descriptions.DescriptionEditActivity.Action;
import org.wikipedia.json.GsonUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_CAPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_DESCRIPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_IMAGE_TAGS;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_CAPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION;

public final class SuggestedEditsFunnel extends TimedFunnel {
    private static SuggestedEditsFunnel INSTANCE;

    private static final String SCHEMA_NAME = "MobileWikiAppSuggestedEdits";
    private static final int REV_ID = 18949003;

    private static final String SUGGESTED_EDITS_UI_VERSION = "1.0";
    private static final String SUGGESTED_EDITS_API_VERSION = "1.0";

    public static final String SUGGESTED_EDITS_ADD_COMMENT = "#suggestededit-add " + SUGGESTED_EDITS_UI_VERSION;
    public static final String SUGGESTED_EDITS_TRANSLATE_COMMENT = "#suggestededit-translate " + SUGGESTED_EDITS_UI_VERSION;
    public static final String SUGGESTED_EDITS_IMAGE_TAG_AUTO_COMMENT = "#suggestededit-imgtag-auto " + SUGGESTED_EDITS_UI_VERSION;
    public static final String SUGGESTED_EDITS_IMAGE_TAG_CUSTOM_COMMENT = "#suggestededit-imgtag-custom " + SUGGESTED_EDITS_UI_VERSION;

    private InvokeSource invokeSource;
    private String parentSessionToken;
    private int helpOpenedCount = 0;
    private int contributionsOpenedCount = 0;
    private SuggestedEditStatsCollection statsCollection = new SuggestedEditStatsCollection();
    private List<String> uniqueTitles = new ArrayList<>();

    private SuggestedEditsFunnel(WikipediaApp app, InvokeSource invokeSource) {
        super(app, SCHEMA_NAME, REV_ID, Funnel.SAMPLE_LOG_ALL);
        this.invokeSource = invokeSource;
        this.parentSessionToken = app.getSessionFunnel().getSessionToken();
    }

    public static SuggestedEditsFunnel get(InvokeSource invokeSource) {
        if (INSTANCE == null) {
            INSTANCE = new SuggestedEditsFunnel(WikipediaApp.getInstance(), invokeSource);
        } else if (INSTANCE.invokeSource != invokeSource) {
            INSTANCE.log();
            INSTANCE = new SuggestedEditsFunnel(WikipediaApp.getInstance(), invokeSource);
        }
        return INSTANCE;
    }

    public static SuggestedEditsFunnel get() {
        if (INSTANCE != null && INSTANCE.invokeSource != SUGGESTED_EDITS) {
            return INSTANCE;
        }
        return get(SUGGESTED_EDITS);
    }

    public static void reset() {
        INSTANCE = null;
    }

    @Override protected void preprocessSessionToken(@NonNull JSONObject eventData) {
        preprocessData(eventData, "session_token", parentSessionToken);
    }

    public void impression(Action action) {
        if (action == ADD_DESCRIPTION) {
            statsCollection.addDescriptionStats.impressions++;
        } else if (action == TRANSLATE_DESCRIPTION) {
            statsCollection.translateDescriptionStats.impressions++;
        } else if (action == ADD_CAPTION) {
            statsCollection.addCaptionStats.impressions++;
        } else if (action == TRANSLATE_CAPTION) {
            statsCollection.translateCaptionStats.impressions++;
        } else if (action == ADD_IMAGE_TAGS) {
            statsCollection.machineImageTagStats.impressions++;
        }
    }


    public void click(String title, Action action) {
        SuggestedEditStats stats;
        if (action == ADD_DESCRIPTION) {
            stats = statsCollection.addDescriptionStats;
        } else if (action == TRANSLATE_DESCRIPTION) {
            stats = statsCollection.translateDescriptionStats;
        } else if (action == ADD_CAPTION) {
            stats = statsCollection.addCaptionStats;
        } else if (action == TRANSLATE_CAPTION) {
            stats = statsCollection.translateCaptionStats;
        } else {
            return;
        }
        stats.clicks++;
        if (!uniqueTitles.contains(title)) {
            uniqueTitles.add(title);
            final int maxItems = 100;
            if (uniqueTitles.size() > maxItems) {
                uniqueTitles.remove(0);
            }
            stats.suggestionsClicked++;
        }
    }

    public void cancel(Action action) {
        if (action == ADD_DESCRIPTION) {
            statsCollection.addDescriptionStats.cancels++;
        } else if (action == TRANSLATE_DESCRIPTION) {
            statsCollection.translateDescriptionStats.cancels++;
        } else if (action == ADD_CAPTION) {
            statsCollection.addCaptionStats.cancels++;
        } else if (action == TRANSLATE_CAPTION) {
            statsCollection.translateCaptionStats.cancels++;
        }
    }

    public void success(Action action) {
        if (action == ADD_DESCRIPTION) {
            statsCollection.addDescriptionStats.successes++;
        } else if (action == TRANSLATE_DESCRIPTION) {
            statsCollection.translateDescriptionStats.successes++;
        } else if (action == ADD_CAPTION) {
            statsCollection.addCaptionStats.successes++;
        } else if (action == TRANSLATE_CAPTION) {
            statsCollection.translateCaptionStats.successes++;
        } else if (action == ADD_IMAGE_TAGS) {
            statsCollection.machineImageTagStats.successes++;
        }
    }

    public void failure(Action action) {
        if (action == ADD_DESCRIPTION) {
            statsCollection.addDescriptionStats.failures++;
        } else if (action == TRANSLATE_DESCRIPTION) {
            statsCollection.translateDescriptionStats.failures++;
        } else if (action == ADD_CAPTION) {
            statsCollection.addCaptionStats.failures++;
        } else if (action == TRANSLATE_CAPTION) {
            statsCollection.translateCaptionStats.failures++;
        } else if (action == ADD_IMAGE_TAGS) {
            statsCollection.machineImageTagStats.failures++;
        }
    }

    public void helpOpened() {
        helpOpenedCount++;
    }

    public void log() {
        try {
            JSONObject jsonObject = new JSONObject(GsonUtil.getDefaultGson().toJson(statsCollection));
            removeStatsWithNoValues(jsonObject);
            log(
                    "edit_tasks", jsonObject,
                    "help_opened", helpOpenedCount,
                    "source", (invokeSource.getName())
            );
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void removeStatsWithNoValues(JSONObject editTasksJSONObject) {
        ArrayList<String> editTaskKeysToRemove = new ArrayList<>();
        try {
            Iterator<String> editTaskKeys = editTasksJSONObject.keys();
            while (editTaskKeys.hasNext()) {
                String key = editTaskKeys.next();

                if (editTasksJSONObject.get(key) instanceof JSONObject) {
                    JSONObject editTaskInnerTypeJSONObject = (JSONObject) editTasksJSONObject.get(key);
                    Iterator<String> innerJSONObjectKeys = editTaskInnerTypeJSONObject.keys();
                    boolean noValue = true;

                    while (innerJSONObjectKeys.hasNext()) {
                        String keyinner = innerJSONObjectKeys.next();
                        if (!(editTaskInnerTypeJSONObject.getInt(keyinner) == 0)) {
                            noValue = false;
                        }
                    }
                    if (noValue) {
                        editTaskKeysToRemove.add(key);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        for (String keyy : editTaskKeysToRemove) {
            editTasksJSONObject.remove(keyy);
        }
    }

    private static class SuggestedEditStatsCollection {
        @SerializedName("a-d") private SuggestedEditStats addDescriptionStats = new SuggestedEditStats();
        @SerializedName("t-d") private SuggestedEditStats translateDescriptionStats = new SuggestedEditStats();
        @SerializedName("a-c") private SuggestedEditStats addCaptionStats = new SuggestedEditStats();
        @SerializedName("t-c") private SuggestedEditStats translateCaptionStats = new SuggestedEditStats();
        @SerializedName("i-t") private SuggestedEditStats machineImageTagStats = new SuggestedEditStats();
    }

    @SuppressWarnings("unused")
    private static class SuggestedEditStats {
        @SerializedName("imp")private int impressions;
        @SerializedName("clk")private int clicks;
        @SerializedName("sug_clkd") private int suggestionsClicked;
        @SerializedName("cncl")private int cancels;
        @SerializedName("suc")private int successes;
        @SerializedName("fl")private int failures;
    }
}
