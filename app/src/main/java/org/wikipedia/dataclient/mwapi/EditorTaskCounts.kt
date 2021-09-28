package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs

@Serializable
class EditorTaskCounts {

    @SerialName("revert_counts") private val revertCounts: JsonElement? = null
    @SerialName("edit_streak") private val editStreak: JsonElement? = null

    private val counts: JsonElement? = null

    private val descriptionEditsPerLanguage: Map<String, Int>
        get() {
            var editsPerLanguage: Map<String, Int>? = null
            if (counts != null && counts !is JsonArray) {
                editsPerLanguage = JsonUtil.json.decodeFromJsonElement<Counts>(counts).appDescriptionEdits
            }
            return editsPerLanguage ?: emptyMap()
        }

    private val captionEditsPerLanguage: Map<String, Int>
        get() {
            var editsPerLanguage: Map<String, Int>? = null
            if (counts != null && counts !is JsonArray) {
                editsPerLanguage = JsonUtil.json.decodeFromJsonElement<Counts>(counts).appCaptionEdits
            }
            return editsPerLanguage ?: emptyMap()
        }

    private val descriptionRevertsPerLanguage: Map<String, Int>
        get() {
            var revertsPerLanguage: Map<String, Int>? = null
            if (revertCounts != null && revertCounts !is JsonArray) {
                revertsPerLanguage = JsonUtil.json.decodeFromJsonElement<Counts>(revertCounts).appDescriptionEdits
            }
            return revertsPerLanguage ?: emptyMap()
        }

    private val captionRevertsPerLanguage: Map<String, Int>
        get() {
            var revertsPerLanguage: Map<String, Int>? = null
            if (revertCounts != null && revertCounts !is JsonArray) {
                revertsPerLanguage = JsonUtil.json.decodeFromJsonElement<Counts>(revertCounts).appCaptionEdits
            }
            return revertsPerLanguage ?: emptyMap()
        }

    private val totalDepictsReverts: Int
        get() {
            var revertsPerLanguage: Map<String, Int>? = null
            if (revertCounts != null && revertCounts !is JsonArray) {
                revertsPerLanguage = JsonUtil.json.decodeFromJsonElement<Counts>(revertCounts).appDepictsEdits
            }
            return revertsPerLanguage?.get("*") ?: 0
        }

    val totalDepictsEdits: Int
        get() {
            var editsPerLanguage: Map<String, Int>? = null
            if (counts != null && counts !is JsonArray) {
                editsPerLanguage = JsonUtil.json.decodeFromJsonElement<Counts>(counts).appDepictsEdits
            }
            return editsPerLanguage?.get("*") ?: 0
        }

    val totalEdits: Int
        get() {
            return if (Prefs.shouldOverrideSuggestedEditCounts()) {
                Prefs.overrideSuggestedEditCount
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
                Prefs.overrideSuggestedRevertCount
            } else {
                descriptionRevertsPerLanguage.values.sum() + captionRevertsPerLanguage.values.sum() + totalDepictsReverts
            }
        }

    @Serializable
    class Counts {

        @SerialName("app_description_edits") val appDescriptionEdits: Map<String, Int>? = null
        @SerialName("app_caption_edits") val appCaptionEdits: Map<String, Int>? = null
        @SerialName("app_depicts_edits") val appDepictsEdits: Map<String, Int>? = null
    }
}
