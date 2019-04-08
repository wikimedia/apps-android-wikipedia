package org.wikipedia.dataclient.mwapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import org.wikipedia.json.GsonUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;

@SuppressWarnings("unused")
public class EditorTaskCounts {
    @Nullable private JsonElement counts;
    @Nullable @SerializedName("targets_passed") private JsonElement targetsPassed;
    @Nullable private JsonElement targets;

    @Nullable
    public Map<String, Integer> getDescriptionEditsPerLanguage() {
        if (counts != null && !(counts instanceof JsonArray)) {
            return GsonUtil.getDefaultGson().fromJson(counts, Counts.class).appDescriptionEdits;
        }
        return Collections.emptyMap();
    }

    @Nullable
    public List<Integer> getDescriptionEditTargetsPassed() {
        if (targetsPassed != null && !(targetsPassed instanceof JsonArray)) {
            return GsonUtil.getDefaultGson().fromJson(targetsPassed, Targets.class).appDescriptionEdits;
        }
        return Collections.emptyList();
    }

    @Nullable
    public List<Integer> getDescriptionEditTargets() {
        if (targets != null && !(targets instanceof JsonArray)) {
            return GsonUtil.getDefaultGson().fromJson(targets, Targets.class).appDescriptionEdits;
        }
        return Collections.emptyList();
    }

    public class Counts {
        @Nullable @SerializedName("app_description_edits") private Map<String, Integer> appDescriptionEdits;
    }

    public class Targets {
        @Nullable @SerializedName("app_description_edits") private List<Integer> appDescriptionEdits;
    }
}
