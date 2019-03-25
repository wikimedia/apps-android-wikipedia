package org.wikipedia.dataclient.mwapi;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class EditorTaskCounts {
    @Nullable private Counts counts;
    @Nullable @SerializedName("targets_passed") private Targets targetsPassed;

    @Nullable
    public Map<String, Integer> getDescriptionEditsPerLanguage() {
        return counts != null && counts.appDescriptionEdits != null ? counts.appDescriptionEdits : null;
    }

    @Nullable
    public List<Integer> getDescriptionEditTargetsPassed() {
        return targetsPassed != null && targetsPassed.appDescriptionEdits != null ? targetsPassed.appDescriptionEdits : null;
    }

    public class Counts {
        @Nullable @SerializedName("app_description_edits") private Map<String, Integer> appDescriptionEdits;
    }

    public class Targets {
        @Nullable @SerializedName("app_description_edits") private List<Integer> appDescriptionEdits;
    }
}
