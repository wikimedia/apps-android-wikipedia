package org.wikipedia.dataclient.restbase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class DiffResponse(internal val from: DiffRevision = DiffRevision(), internal val to: DiffRevision = DiffRevision(),
                   @Json(name = "diff") val diffs: List<DiffItem> = emptyList()) {

    @JsonClass(generateAdapter = true)
    class DiffItem(val type: Int = 0, internal val lineNumber: Int = 0,
                   val text: String = "", internal val offset: DiffOffset = DiffOffset(),
                   val highlightRanges: List<HighlightRange> = emptyList())

    @JsonClass(generateAdapter = true)
    class DiffOffset(internal val from: Int = 0, internal val to: Int = 0)

    @JsonClass(generateAdapter = true)
    class HighlightRange(val start: Int = 0, val length: Int = 0, val type: Int = 0)

    @JsonClass(generateAdapter = true)
    class DiffRevision(internal val id: Long = 0, @Json(name = "slot_role") internal val slotRole: String = "",
                       internal val sections: List<RevisionSection> = emptyList())

    @JsonClass(generateAdapter = true)
    class RevisionSection(internal val level: Int = 0, internal val heading: String = "", internal val offset: Int = 0)

    companion object {
        // A line with the same content in both revisions, included to provide context when viewing the diff.
        // The API returns up to two context lines around each change.
        const val DIFF_TYPE_LINE_WITH_SAME_CONTENT = 0

        // A line included in the to revision but not in the from revision.
        const val DIFF_TYPE_LINE_ADDED = 1

        // A line included in the from revision but not in the to revision.
        const val DIFF_TYPE_LINE_REMOVED = 2

        // A line containing text that differs between the two revisions.
        // (For changes to paragraph location as well as content, see type 5.)
        const val DIFF_TYPE_LINE_WITH_DIFF = 3

        // When a paragraph's location differs between the two revisions, this represents the location
        // in the from revision.
        const val DIFF_TYPE_PARAGRAPH_MOVED_FROM = 4

        // When a paragraph's location differs between the two revisions, this represents the location
        // in the to revision. This type can also include word-level differences between the two revisions.
        const val DIFF_TYPE_PARAGRAPH_MOVED_TO = 5
        const val HIGHLIGHT_TYPE_ADD = 0
        const val HIGHLIGHT_TYPE_DELETE = 1
    }
}
