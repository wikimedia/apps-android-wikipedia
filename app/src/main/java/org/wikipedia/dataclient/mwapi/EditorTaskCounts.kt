package org.wikipedia.dataclient.mwapi

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.wikipedia.json.GsonUtil
import org.wikipedia.settings.Prefs

class EditorTaskCounts {

    @SerializedName("revert_counts") private val revertCounts: JsonElement? = null
    @SerializedName("edit_streak") private val editStreak: JsonElement? = null

    private val counts: JsonElement? = null

    private val descriptionEditsPerLanguage: Map<String, Int>
        get() {
            var editsPerLanguage: Map<String, Int>? = null
            if (counts != null && counts !is JsonArray) {
                editsPerLanguage =
                    GsonUtil.getDefaultGson().fromJson(counts, Counts::class.java).appDescriptionEdits
            }
            return editsPerLanguage ?: emptyMap()
        }

    private val captionEditsPerLanguage: Map<String, Int>
        get() {
            var editsPerLanguage: Map<String, Int>? = null
            if (counts != null && counts !is JsonArray) {
                editsPerLanguage =
                    GsonUtil.getDefaultGson().fromJson(counts, Counts::class.java).appCaptionEdits
            }
            return editsPerLanguage ?: emptyMap()
        }

    private val descriptionRevertsPerLanguage: Map<String, Int>
        get() {
            var revertsPerLanguage: Map<String, Int>? = null
            if (revertCounts != null && revertCounts !is JsonArray) {
                revertsPerLanguage =
                    GsonUtil.getDefaultGson().fromJson(revertCounts, Counts::class.java).appDescriptionEdits
            }
            return revertsPerLanguage ?: emptyMap()
        }

    private val captionRevertsPerLanguage: Map<String, Int>
        get() {
            var revertsPerLanguage: Map<String, Int>? = null
            if (revertCounts != null && revertCounts !is JsonArray) {
                revertsPerLanguage =
                    GsonUtil.getDefaultGson().fromJson(revertCounts, Counts::class.java).appCaptionEdits
            }
            return revertsPerLanguage ?: emptyMap()
        }

    private val totalDepictsReverts: Int
        get() {
            var revertsPerLanguage: Map<String, Int>? = null
            if (revertCounts != null && revertCounts !is JsonArray) {
                revertsPerLanguage =
                    GsonUtil.getDefaultGson().fromJson(revertCounts, Counts::class.java).appDepictsEdits
            }
            return revertsPerLanguage?.get("*") ?: 0
        }

    val totalDepictsEdits: Int
        get() {
            var editsPerLanguage: Map<String, Int>? = null
            if (counts != null && counts !is JsonArray) {
                editsPerLanguage =
                    GsonUtil.getDefaultGson().fromJson(counts, Counts::class.java).appDepictsEdits
            }
            return editsPerLanguage?.get("*") ?: 0
        }

    val totalEdits: Int
        get() {
            return if (Prefs.shouldOverrideSuggestedEditCounts()) {
                Prefs.getOverrideSuggestedEditCount()
            } else {
                descriptionEditsPerLanguage.values.sum() + captionEditsPerLanguage.values.sum() + totalDepictsEdits
            }
        }

    val totalDescriptionEdits: Int
        get() = descriptionEditsPerLanguage.values.sum()

    val totalImageCaptionEdits: Int
        get() = captionEditsPerLanguage.values.sum()

    val totalReverts: Int
        get() {
            return if (Prefs.shouldOverrideSuggestedEditCounts()) {
                Prefs.getOverrideSuggestedRevertCount()
            } else {
                descriptionRevertsPerLanguage.values.sum() + captionRevertsPerLanguage.values.sum() + totalDepictsReverts
            }
        }

    class Counts {

        @SerializedName("app_description_edits") val appDescriptionEdits: Map<String, Int>? = null
        @SerializedName("app_caption_edits") val appCaptionEdits: Map<String, Int>? = null
        @SerializedName("app_depicts_edits") val appDepictsEdits: Map<String, Int>? = null
    }
}
