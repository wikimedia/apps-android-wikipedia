package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil.dbDateParse
import java.util.*

@JsonClass(generateAdapter = true)
class EditorTaskCounts(
    internal val counts: Any? = null,
    @Json(name = "revert_counts") internal val revertCounts: Any? = null,
    @Json(name = "edit_streak") internal val editStreak: Any? = null
) {
    internal val descriptionEditsPerLanguage: Map<String, Int>
        get() = (counts as? Counts)?.appDescriptionEdits ?: emptyMap()

    internal val captionEditsPerLanguage: Map<String, Int>
        get() = (counts as? Counts)?.appCaptionEdits ?: emptyMap()

    val totalDepictsEdits: Int
        get() = (counts as? Counts)?.appDepictsEdits?.get("*") ?: 0

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
        get() = (revertCounts as? Counts)?.appDescriptionEdits ?: emptyMap()

    internal val captionRevertsPerLanguage: Map<String, Int>
        get() = (revertCounts as? Counts)?.appCaptionEdits ?: emptyMap()

    internal val totalDepictsReverts: Int
        get() = (revertCounts as? Counts)?.appDepictsEdits?.get("*") ?: 0

    val totalReverts: Int
        get() = if (Prefs.shouldOverrideSuggestedEditCounts()) {
            Prefs.getOverrideSuggestedRevertCount()
        } else {
            descriptionRevertsPerLanguage.values.sum() + captionRevertsPerLanguage.values.sum() + totalDepictsReverts
        }

    val editStreakLength: Int
        get() = (editStreak as? EditStreak)?.length ?: 0

    val lastEditDate: Date
        get() = (editStreak as? EditStreak)?.let { dbDateParse(it.lastEditTime) } ?: Date(0)

    @JsonClass(generateAdapter = true)
    class Counts(
        @Json(name = "app_description_edits") val appDescriptionEdits: Map<String, Int> = emptyMap(),
        @Json(name = "app_caption_edits") val appCaptionEdits: Map<String, Int> = emptyMap(),
        @Json(name = "app_depicts_edits") val appDepictsEdits: Map<String, Int> = emptyMap()
    )

    @JsonClass(generateAdapter = true)
    class EditStreak(val length: Int = 0, @Json(name = "last_edit_time") val lastEditTime: String = "")
}
