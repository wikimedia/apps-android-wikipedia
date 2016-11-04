package org.wikipedia.edit.summaries;

import java.util.Date;

public class EditSummary {
    public static final EditSummaryDatabaseTable DATABASE_TABLE = new EditSummaryDatabaseTable();
    private final String summary;
    private final Date lastUsed;

    public EditSummary(String summary, Date lastUsed) {
        this.summary = summary;
        this.lastUsed = lastUsed;
    }

    public String getSummary() {
        return summary;
    }

    public Date getLastUsed() {
        return lastUsed;
    }
}
