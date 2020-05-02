package org.wikipedia.edit.summaries;

import org.threeten.bp.Instant;

public class EditSummary {
    public static final EditSummaryDatabaseTable DATABASE_TABLE = new EditSummaryDatabaseTable();
    private final String summary;
    private final Instant lastUsed;

    public EditSummary(String summary, Instant lastUsed) {
        this.summary = summary;
        this.lastUsed = lastUsed;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getLastUsed() {
        return lastUsed;
    }
}
