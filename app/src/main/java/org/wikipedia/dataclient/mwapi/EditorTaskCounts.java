package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import org.wikipedia.json.GsonUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @NonNull
    public List<Integer> getDescriptionEditTargetsPassed() {
        List<Integer> passedList = null;
        if (targetsPassed != null && !(targetsPassed instanceof JsonArray)) {
            passedList = GsonUtil.getDefaultGson().fromJson(targetsPassed, Targets.class).appDescriptionEdits;
        }
        return passedList == null ? Collections.emptyList() : passedList;
    }

    public int getDescriptionEditTargetsPassedCount() {
        List<Integer> targetList = getDescriptionEditTargets();
        List<Integer> passedList = getDescriptionEditTargetsPassed();
        int count = 0;
        if (targetList != null && !targetList.isEmpty() && !passedList.isEmpty()) {
            for (int target : targetList) {
                if (passedList.contains(target)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Nullable
    public List<Integer> getDescriptionEditTargets() {
        if (targets != null && !(targets instanceof JsonArray)) {
            return GsonUtil.getDefaultGson().fromJson(targets, Targets.class).appDescriptionEdits;
        }
        return Collections.emptyList();
    }

    @Nullable
    public Map<String, Integer> getCaptionEditsPerLanguage() {
        if (counts != null && !(counts instanceof JsonArray)) {
            return GsonUtil.getDefaultGson().fromJson(counts, Counts.class).appCaptionEdits;
        }
        return Collections.emptyMap();
    }

    @NonNull
    public List<Integer> getCaptionEditTargetsPassed() {
        List<Integer> passedList = null;
        if (targetsPassed != null && !(targetsPassed instanceof JsonArray)) {
            passedList = GsonUtil.getDefaultGson().fromJson(targetsPassed, Targets.class).appCaptionEdits;
        }
        return passedList == null ? Collections.emptyList() : passedList;
    }

    public int getCaptionEditTargetsPassedCount() {
        List<Integer> targetList = getCaptionEditTargets();
        List<Integer> passedList = getCaptionEditTargetsPassed();
        int count = 0;
        if (targetList != null && !targetList.isEmpty() && !passedList.isEmpty()) {
            for (int target : targetList) {
                if (passedList.contains(target)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Nullable
    public List<Integer> getCaptionEditTargets() {
        if (targets != null && !(targets instanceof JsonArray)) {
            return GsonUtil.getDefaultGson().fromJson(targets, Targets.class).appCaptionEdits;
        }
        return Collections.emptyList();
    }

    public class Counts {
        @Nullable @SerializedName("app_description_edits") private Map<String, Integer> appDescriptionEdits;
        @Nullable @SerializedName("app_caption_edits") private Map<String, Integer> appCaptionEdits;
    }

    public class Targets {
        @Nullable @SerializedName("app_description_edits") private List<Integer> appDescriptionEdits;
        @Nullable @SerializedName("app_caption_edits") private List<Integer> appCaptionEdits;
    }
}
