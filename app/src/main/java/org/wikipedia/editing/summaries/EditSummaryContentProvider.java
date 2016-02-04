package org.wikipedia.editing.summaries;

import org.wikipedia.database.SQLiteContentProvider;

public class EditSummaryContentProvider extends SQLiteContentProvider {
    public EditSummaryContentProvider() {
        super(EditSummary.DATABASE_TABLE);
    }
}
