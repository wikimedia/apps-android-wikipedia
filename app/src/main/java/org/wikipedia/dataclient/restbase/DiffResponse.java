package org.wikipedia.dataclient.restbase;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class DiffResponse {
    // A line with the same content in both revisions, included to provide context when viewing the diff.
    // The API returns up to two context lines around each change.
    public static final int DIFF_TYPE_LINE_WITH_SAME_CONTENT = 0;

    // A line included in the to revision but not in the from revision.
    public static final int DIFF_TYPE_LINE_ADDED = 1;

    // A line included in the from revision but not in the to revision.
    public static final int DIFF_TYPE_LINE_REMOVED = 2;

    // A line containing text that differs between the two revisions.
    // (For changes to paragraph location as well as content, see type 5.)
    public static final int DIFF_TYPE_LINE_WITH_DIFF = 3;

    // When a paragraph's location differs between the two revisions, this represents the location
    // in the from revision.
    public static final int DIFF_TYPE_PARAGRAPH_MOVED_FROM = 4;

    // When a paragraph's location differs between the two revisions, this represents the location
    // in the to revision. This type can also include word-level differences between the two revisions.
    public static final int DIFF_TYPE_PARAGRAPH_MOVED_TO = 5;

    public static final int HIGHLIGHT_TYPE_ADD = 0;
    public static final int HIGHLIGHT_TYPE_DELETE = 1;

    private DiffRevision from;
    private DiffRevision to;
    private List<DiffItem> diff;

    public List<DiffItem> getDiffs() {
        return diff != null ? diff : Collections.emptyList();
    }

    public static class DiffItem {
        private int type;
        private int lineNumber;
        private String text;
        private DiffOffset offset;
        private List<HighlightRange> highlightRanges;

        public int getType() {
            return type;
        }

        public String getText() {
            return StringUtils.defaultString(text);
        }

        public List<HighlightRange> getHighlightRanges() {
            return highlightRanges != null ? highlightRanges : Collections.emptyList();
        }
    }

    public static class DiffOffset {
        private int from;
        private int to;
    }

    public static class HighlightRange {
        private int start;
        private int length;
        private int type;

        public int getStart() {
            return start;
        }

        public int getLength() {
            return length;
        }

        public int getType() {
            return type;
        }
    }

    public static class DiffRevision {
        private long id;
        @Nullable @SerializedName("slot_role") private String slotRole;
        @Nullable private List<RevisionSection> sections;
    }

    public static class RevisionSection {
        private int level;
        @Nullable private String heading;
        private int offset;
    }
}
