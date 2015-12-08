package org.wikipedia.editing.summaries;

import org.wikipedia.data.SQLiteContentProvider;

public class EditSummaryContentProvider extends SQLiteContentProvider<EditSummary> {
    public EditSummaryContentProvider() {
        super(EditSummary.PERSISTENCE_HELPER);
    }
}
