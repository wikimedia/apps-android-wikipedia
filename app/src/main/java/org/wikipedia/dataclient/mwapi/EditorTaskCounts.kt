package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil.dbDateParse
import java.util.*

@JsonClass(generateAdapter = true)
class EditorTaskCounts(
    internal val counts: Counts? = null,
    @Json(name = "revert_counts") internal val revertCounts: Counts? = null,
    @Json(name = "edit_streak") internal val editStreak: EditStreak? = null
) {
    internal val descriptionEditsPerLanguage: Map<String, Int>
        get() = counts?.appDescriptionEdits ?: emptyMap()

    internal val captionEditsPerLanguage: Map<String, Int>
        get() = counts?.appCaptionEdits ?: emptyMap()

    val totalDepictsEdits: Int
        get() = counts?.appDepictsEdits?.get("*") ?: 0

    val totalEdits: Int
        get() = if (Prefs.shouldOverrideSuggestedEditCounts()) {
            Prefs.getOverrideSuggestedEditCount()
        } else {
            descriptionEditsPerLanguage.values.sum() + captionEditsPerLanguage.values.sum() + totalDepictsEdits
        }

    val totalDescriptionEdits: Int
        get() = descriptionEditsPerLanguage.values.sum()

    val totalImageCaptionEdits: Int
        get() = captionEditsPerLanguage.values.sum()

    internal val descriptionRevertsPerLanguage: Map<String, Int>
        get() = revertCounts?.appDescriptionEdits ?: emptyMap()

    internal val captionRevertsPerLanguage: Map<String, Int>
        get() = revertCounts?.appCaptionEdits ?: emptyMap()

    internal val totalDepictsReverts: Int
        get() = revertCounts?.appDepictsEdits?.get("*") ?: 0

    val totalReverts: Int
        get() = if (Prefs.shouldOverrideSuggestedEditCounts()) {
            Prefs.getOverrideSuggestedRevertCount()
        } else {
            descriptionRevertsPerLanguage.values.sum() + captionRevertsPerLanguage.values.sum() + totalDepictsReverts
        }

    val editStreakLength: Int
        get() = editStreak?.length ?: 0

    val lastEditDate: Date
        get() = editStreak?.let { dbDateParse(it.lastEditTime) } ?: Date(0)

    @JsonClass(generateAdapter = true)
    class Counts(
        @Json(name = "app_description_edits") val appDescriptionEdits: Map<String, Int> = emptyMap(),
        @Json(name = "app_caption_edits") val appCaptionEdits: Map<String, Int> = emptyMap(),
        @Json(name = "app_depicts_edits") val appDepictsEdits: Map<String, Int> = emptyMap()
    )

    @JsonClass(generateAdapter = true)
    class EditStreak(val length: Int = 0, @Json(name = "last_edit_time") val lastEditTime: String = "")
}
