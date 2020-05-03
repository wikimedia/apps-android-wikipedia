package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.collections4.MapUtils;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DateUtil;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

@SuppressWarnings("unused")
public class EditorTaskCounts {
    @Nullable private JsonElement counts;
    @Nullable @SerializedName("revert_counts") private JsonElement revertCounts;
    @Nullable @SerializedName("edit_streak") private JsonElement editStreak;

    @NonNull
    private Map<String, Integer> getDescriptionEditsPerLanguage() {
        Map<String, Integer> editsPerLanguage = null;
        if (counts != null && !(counts instanceof JsonArray)) {
            editsPerLanguage = GsonUtil.getDefaultGson().fromJson(counts, Counts.class).appDescriptionEdits;
        }
        return MapUtils.emptyIfNull(editsPerLanguage);
    }

    @NonNull
    private Map<String, Integer> getCaptionEditsPerLanguage() {
        Map<String, Integer> editsPerLanguage = null;
        if (counts != null && !(counts instanceof JsonArray)) {
            editsPerLanguage = GsonUtil.getDefaultGson().fromJson(counts, Counts.class).appCaptionEdits;
        }
        return MapUtils.emptyIfNull(editsPerLanguage);
    }

    private int getTotalDepictsEdits() {
        Map<String, Integer> editsPerLanguage = null;
        if (counts != null && !(counts instanceof JsonArray)) {
            editsPerLanguage = GsonUtil.getDefaultGson().fromJson(counts, Counts.class).appDepictsEdits;
        }
        return MapUtils.getIntValue(editsPerLanguage, "*");
    }

    public int getTotalEdits() {
        int totalEdits = 0;
        for (int count : getDescriptionEditsPerLanguage().values()) {
            totalEdits += count;
        }
        for (int count : getCaptionEditsPerLanguage().values()) {
            totalEdits += count;
        }
        totalEdits += getTotalDepictsEdits();
        if (Prefs.shouldOverrideSuggestedEditCounts()) {
            totalEdits = Prefs.getOverrideSuggestedEditCount();
        }
        return totalEdits;
    }

    @NonNull
    private Map<String, Integer> getDescriptionRevertsPerLanguage() {
        Map<String, Integer> revertsPerLanguage = null;
        if (revertCounts != null && !(revertCounts instanceof JsonArray)) {
            revertsPerLanguage = GsonUtil.getDefaultGson().fromJson(revertCounts, Counts.class).appDescriptionEdits;
        }
        return MapUtils.emptyIfNull(revertsPerLanguage);
    }

    @NonNull
    private Map<String, Integer> getCaptionRevertsPerLanguage() {
        Map<String, Integer> revertsPerLanguage = null;
        if (revertCounts != null && !(revertCounts instanceof JsonArray)) {
            revertsPerLanguage = GsonUtil.getDefaultGson().fromJson(revertCounts, Counts.class).appCaptionEdits;
        }
        return MapUtils.emptyIfNull(revertsPerLanguage);
    }

    private int getTotalDepictsReverts() {
        Map<String, Integer> revertsPerLanguage = null;
        if (revertCounts != null && !(revertCounts instanceof JsonArray)) {
            revertsPerLanguage = GsonUtil.getDefaultGson().fromJson(revertCounts, Counts.class).appDepictsEdits;
        }
        return MapUtils.getIntValue(revertsPerLanguage, "*");
    }

    public int getTotalReverts() {
        int totalReverts = 0;
        for (int count : getDescriptionRevertsPerLanguage().values()) {
            totalReverts += count;
        }
        for (int count : getCaptionRevertsPerLanguage().values()) {
            totalReverts += count;
        }
        totalReverts += getTotalDepictsReverts();
        if (Prefs.shouldOverrideSuggestedEditCounts()) {
            totalReverts = Prefs.getOverrideSuggestedRevertCount();
        }
        return totalReverts;
    }

    public int getEditStreak() {
        if (editStreak == null || (editStreak instanceof JsonArray)) {
            return 0;
        }
        EditStreak streak = GsonUtil.getDefaultGson().fromJson(editStreak, EditStreak.class);
        return streak.length;
    }

    @NonNull
    public Date getLastEditDate() {
        Date date = new Date(0);
        if (editStreak == null || (editStreak instanceof JsonArray)) {
            return date;
        }
        EditStreak streak = GsonUtil.getDefaultGson().fromJson(editStreak, EditStreak.class);
        try {
            date = DateUtil.dbDateParse(streak.lastEditTime);
        } catch (ParseException e) {
            // ignore
        }
        return date;
    }

    public static class Counts {
        @Nullable @SerializedName("app_description_edits") private Map<String, Integer> appDescriptionEdits;
        @Nullable @SerializedName("app_caption_edits") private Map<String, Integer> appCaptionEdits;
        @Nullable @SerializedName("app_depicts_edits") private Map<String, Integer> appDepictsEdits;
    }

    private static class EditStreak {
        private int length;
        @Nullable @SerializedName("last_edit_time") private String lastEditTime;
    }
}
