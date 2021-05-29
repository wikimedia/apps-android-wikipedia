package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DateUtil;

import java.time.LocalDate;
import java.util.Collections;
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
        return editsPerLanguage == null ? Collections.emptyMap() : editsPerLanguage;
    }

    @NonNull
    private Map<String, Integer> getCaptionEditsPerLanguage() {
        Map<String, Integer> editsPerLanguage = null;
        if (counts != null && !(counts instanceof JsonArray)) {
            editsPerLanguage = GsonUtil.getDefaultGson().fromJson(counts, Counts.class).appCaptionEdits;
        }
        return editsPerLanguage == null ? Collections.emptyMap() : editsPerLanguage;
    }

    public int getTotalDepictsEdits() {
        Map<String, Integer> editsPerLanguage = null;
        if (counts != null && !(counts instanceof JsonArray)) {
            editsPerLanguage = GsonUtil.getDefaultGson().fromJson(counts, Counts.class).appDepictsEdits;
        }
        return editsPerLanguage == null ? 0 : editsPerLanguage.get("*") == null ? 0 : editsPerLanguage.get("*");
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

    public int getTotalDescriptionEdits() {
        int totalEdits = 0;
        for (int count : getDescriptionEditsPerLanguage().values()) {
            totalEdits += count;
        }
        return totalEdits;
    }

    public int getTotalImageCaptionEdits() {
        int totalEdits = 0;
        for (int count : getCaptionEditsPerLanguage().values()) {
            totalEdits += count;
        }
        return totalEdits;
    }

    @NonNull
    private Map<String, Integer> getDescriptionRevertsPerLanguage() {
        Map<String, Integer> revertsPerLanguage = null;
        if (revertCounts != null && !(revertCounts instanceof JsonArray)) {
            revertsPerLanguage = GsonUtil.getDefaultGson().fromJson(revertCounts, Counts.class).appDescriptionEdits;
        }
        return revertsPerLanguage == null ? Collections.emptyMap() : revertsPerLanguage;
    }

    @NonNull
    private Map<String, Integer> getCaptionRevertsPerLanguage() {
        Map<String, Integer> revertsPerLanguage = null;
        if (revertCounts != null && !(revertCounts instanceof JsonArray)) {
            revertsPerLanguage = GsonUtil.getDefaultGson().fromJson(revertCounts, Counts.class).appCaptionEdits;
        }
        return revertsPerLanguage == null ? Collections.emptyMap() : revertsPerLanguage;
    }

    private int getTotalDepictsReverts() {
        Map<String, Integer> revertsPerLanguage = null;
        if (revertCounts != null && !(revertCounts instanceof JsonArray)) {
            revertsPerLanguage = GsonUtil.getDefaultGson().fromJson(revertCounts, Counts.class).appDepictsEdits;
        }
        return revertsPerLanguage == null ? 0 : revertsPerLanguage.get("*") == null ? 0 : revertsPerLanguage.get("*");
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
    @SuppressWarnings("checkstyle:magicnumber")
    public LocalDate getLastEditDate() {
        if (editStreak == null || (editStreak instanceof JsonArray)) {
            return LocalDate.of(1970, 1, 1);
        }
        EditStreak streak = GsonUtil.getDefaultGson().fromJson(editStreak, EditStreak.class);
        return DateUtil.dbDateTimeParse(StringUtils.defaultString(streak.lastEditTime)).toLocalDate();
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
