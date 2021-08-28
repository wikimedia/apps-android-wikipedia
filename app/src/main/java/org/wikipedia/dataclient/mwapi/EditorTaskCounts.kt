package org.wikipedia.dataclient.mwapi

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.wikipedia.json.GsonUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import java.util.*

@Serializable
class EditorTaskCounts {

    @SerializedName("revert_counts") @Contextual private val revertCounts: JsonElement? = null
    @SerializedName("edit_streak") @Contextual private val editStreak: JsonElement? = null

    @Contextual private val counts: JsonElement? = null

    private val descriptionEditsPerLanguage: Map<String, Int>
        private get() {
            var editsPerLanguage: Map<String, Int>? = null
            if (counts != null && counts !is JsonArray) {
                editsPerLanguage =
                    GsonUtil.getDefaultGson().fromJson(counts, Counts::class.java).appDescriptionEdits
            }
            return editsPerLanguage ?: emptyMap()
        }
    private val captionEditsPerLanguage: Map<String, Int>
        private get() {
            var editsPerLanguage: Map<String, Int>? = null
            if (counts != null && counts !is JsonArray) {
                editsPerLanguage =
                    GsonUtil.getDefaultGson().fromJson(counts, Counts::class.java).appCaptionEdits
            }
            return editsPerLanguage ?: emptyMap()
        }
    val totalDepictsEdits: Int
        get() {
            var editsPerLanguage: Map<String, Int>? = null
            if (counts != null && counts !is JsonArray) {
                editsPerLanguage =
                    GsonUtil.getDefaultGson().fromJson(counts, Counts::class.java).appDepictsEdits
            }
            return if (editsPerLanguage == null) 0 else (if (editsPerLanguage["*"] == null) 0 else editsPerLanguage["*"])!!
        }
    val totalEdits: Int
        get() {
            var totalEdits = 0
            for (count in descriptionEditsPerLanguage.values) {
                totalEdits += count
            }
            for (count in captionEditsPerLanguage.values) {
                totalEdits += count
            }
            totalEdits += totalDepictsEdits
            if (Prefs.shouldOverrideSuggestedEditCounts()) {
                totalEdits = Prefs.getOverrideSuggestedEditCount()
            }
            return totalEdits
        }
    val totalDescriptionEdits: Int
        get() {
            var totalEdits = 0
            for (count in descriptionEditsPerLanguage.values) {
                totalEdits += count
            }
            return totalEdits
        }
    val totalImageCaptionEdits: Int
        get() {
            var totalEdits = 0
            for (count in captionEditsPerLanguage.values) {
                totalEdits += count
            }
            return totalEdits
        }
    private val descriptionRevertsPerLanguage: Map<String, Int>
        private get() {
            var revertsPerLanguage: Map<String, Int>? = null
            if (revertCounts != null && revertCounts !is JsonArray) {
                revertsPerLanguage =
                    GsonUtil.getDefaultGson().fromJson(revertCounts, Counts::class.java).appDescriptionEdits
            }
            return revertsPerLanguage ?: emptyMap()
        }
    private val captionRevertsPerLanguage: Map<String, Int>
        private get() {
            var revertsPerLanguage: Map<String, Int>? = null
            if (revertCounts != null && revertCounts !is JsonArray) {
                revertsPerLanguage =
                    GsonUtil.getDefaultGson().fromJson(revertCounts, Counts::class.java).appCaptionEdits
            }
            return revertsPerLanguage ?: emptyMap()
        }
    private val totalDepictsReverts: Int
        private get() {
            var revertsPerLanguage: Map<String, Int>? = null
            if (revertCounts != null && revertCounts !is JsonArray) {
                revertsPerLanguage =
                    GsonUtil.getDefaultGson().fromJson(revertCounts, Counts::class.java).appDepictsEdits
            }
            return if (revertsPerLanguage == null) 0 else (if (revertsPerLanguage["*"] == null) 0 else revertsPerLanguage["*"])!!
        }
    val totalReverts: Int
        get() {
            var totalReverts = 0
            for (count in descriptionRevertsPerLanguage.values) {
                totalReverts += count
            }
            for (count in captionRevertsPerLanguage.values) {
                totalReverts += count
            }
            totalReverts += totalDepictsReverts
            if (Prefs.shouldOverrideSuggestedEditCounts()) {
                totalReverts = Prefs.getOverrideSuggestedRevertCount()
            }
            return totalReverts
        }

    fun getEditStreak(): Int {
        if (editStreak == null || editStreak is JsonArray) {
            return 0
        }
        val streak = GsonUtil.getDefaultGson().fromJson(editStreak, EditStreak::class.java)
        return streak.length
    }

    val lastEditDate: Date
        get() {
            var date = Date(0)
            if (editStreak == null || editStreak is JsonArray) {
                return date
            }
            val streak = GsonUtil.getDefaultGson().fromJson(editStreak, EditStreak::class.java)
            date = DateUtil.dbDateParse(streak.lastEditTime.orEmpty())
            return date
        }

    @Serializable
    class Counts {

        @SerializedName("app_description_edits") val appDescriptionEdits: Map<String, Int>? = null
        @SerializedName("app_caption_edits") val appCaptionEdits: Map<String, Int>? = null
        @SerializedName("app_depicts_edits") val appDepictsEdits: Map<String, Int>? = null
    }

    @Serializable
    private class EditStreak {

        @SerializedName("last_edit_time") val lastEditTime: String? = null
        val length = 0
    }
}
