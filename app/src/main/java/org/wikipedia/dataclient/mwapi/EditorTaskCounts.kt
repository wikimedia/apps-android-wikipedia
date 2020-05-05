package org.wikipedia.dataclient.mwapi

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.wikipedia.json.GsonUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import java.text.ParseException
import java.util.*

class EditorTaskCounts {
    private val counts: JsonElement? = null

    @SerializedName("revert_counts")
    private val revertCounts: JsonElement? = null

    @SerializedName("edit_streak")
    private val editStreak: JsonElement? = null
    private val descriptionEditsPerLanguage: Map<String?, Int?>
        get() {
            var editsPerLanguage: Map<String?, Int?>? = null
            if (counts != null && counts !is JsonArray) {
                editsPerLanguage = GsonUtil.getDefaultGson().fromJson(counts, Counts::class.java).appDescriptionEdits
            }
            return editsPerLanguage ?: emptyMap()
        }

    private val captionEditsPerLanguage: Map<String?, Int?>
        get() {
            var editsPerLanguage: Map<String?, Int?>? = null
            if (counts != null && counts !is JsonArray) {
                editsPerLanguage = GsonUtil.getDefaultGson().fromJson(counts, Counts::class.java).appCaptionEdits
            }
            return editsPerLanguage ?: emptyMap()
        }

    private val totalDepictsEdits: Int
        get() {
            var editsPerLanguage: Map<String?, Int?>? = null
            if (counts != null && counts !is JsonArray) {
                editsPerLanguage = GsonUtil.getDefaultGson().fromJson(counts, Counts::class.java).appDepictsEdits
            }
            return editsPerLanguage?.get("*") ?: 0
        }

    val totalEdits: Int
        get() {
            var totalEdits = descriptionEditsPerLanguage.values.sumBy { it ?: 0 } +
                    captionEditsPerLanguage.values.sumBy { it ?: 0 }
            totalEdits += totalDepictsEdits
            if (Prefs.shouldOverrideSuggestedEditCounts()) {
                totalEdits = Prefs.getOverrideSuggestedEditCount()
            }
            return totalEdits
        }

    private val descriptionRevertsPerLanguage: Map<String?, Int?>
        get() {
            var revertsPerLanguage: Map<String?, Int?>? = null
            if (revertCounts != null && revertCounts !is JsonArray) {
                revertsPerLanguage = GsonUtil.getDefaultGson().fromJson(revertCounts, Counts::class.java).appDescriptionEdits
            }
            return revertsPerLanguage ?: emptyMap()
        }

    private val captionRevertsPerLanguage: Map<String?, Int?>
        get() {
            var revertsPerLanguage: Map<String?, Int?>? = null
            if (revertCounts != null && revertCounts !is JsonArray) {
                revertsPerLanguage = GsonUtil.getDefaultGson().fromJson(revertCounts, Counts::class.java).appCaptionEdits
            }
            return revertsPerLanguage ?: emptyMap()
        }

    private val totalDepictsReverts: Int
        get() {
            var revertsPerLanguage: Map<String?, Int?>? = null
            if (revertCounts != null && revertCounts !is JsonArray) {
                revertsPerLanguage = GsonUtil.getDefaultGson().fromJson(revertCounts, Counts::class.java).appDepictsEdits
            }
            return revertsPerLanguage?.get("*") ?: 0
        }

    val totalReverts: Int
        get() {
            var totalReverts = descriptionRevertsPerLanguage.values.sumBy { it ?: 0 } +
                    captionRevertsPerLanguage.values.sumBy { it ?: 0 }
            totalReverts += totalDepictsReverts
            if (Prefs.shouldOverrideSuggestedEditCounts()) {
                totalReverts = Prefs.getOverrideSuggestedRevertCount()
            }
            return totalReverts
        }

    val editStreakLength: Int
        get() {
            return if (editStreak == null || editStreak is JsonArray) 0
            else GsonUtil.getDefaultGson().fromJson(editStreak, EditStreak::class.java).length
        }

    // ignore
    val lastEditDate: Date
        get() {
            var date = Date(0)
            if (editStreak == null || editStreak is JsonArray) {
                return date
            }
            val streak = GsonUtil.getDefaultGson().fromJson(editStreak, EditStreak::class.java)
            try {
                date = DateUtil.dbDateParse(streak.lastEditTime)
            } catch (e: ParseException) {
                // ignore
            }
            return date
        }

    class Counts {
        @SerializedName("app_description_edits")
        val appDescriptionEdits: Map<String?, Int?>? = null

        @SerializedName("app_caption_edits")
        val appCaptionEdits: Map<String?, Int?>? = null

        @SerializedName("app_depicts_edits")
        val appDepictsEdits: Map<String?, Int?>? = null
    }

    private class EditStreak {
        val length = 0

        @SerializedName("last_edit_time")
        val lastEditTime: String? = null
    }
}
