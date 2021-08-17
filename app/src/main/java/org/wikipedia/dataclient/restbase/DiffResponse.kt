package org.wikipedia.dataclient.restbase

import com.google.gson.annotations.SerializedName

class DiffResponse {

    private val from: DiffRevision? = null
    private val to: DiffRevision? = null
    val diff: List<DiffItem> = emptyList()

    class DiffItem {

        val type = 0
        private val lineNumber = 0
        val text: String = ""
        private val offset: DiffOffset? = null
        val highlightRanges: List<HighlightRange> = emptyList()
    }

    class DiffOffset {

        private val from = 0
        private val to = 0
    }

    class HighlightRange {

        val start = 0
        val length = 0
        val type = 0
    }

    class DiffRevision {

        private val id: Long = 0

        @SerializedName("slot_role")
        private val slotRole: String? = null
        private val sections: List<RevisionSection>? = null
    }

    class RevisionSection {

        private val level = 0
        private val heading: String? = null
        private val offset = 0
    }

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
