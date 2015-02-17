package org.wikipedia.editing.summaries;

import org.wikipedia.WikipediaApp;
import org.wikipedia.data.DBOpenHelper;
import org.wikipedia.data.SQLiteContentProvider;

public class EditSummaryContentProvider extends SQLiteContentProvider<EditSummary> {
    public EditSummaryContentProvider() {
        super(EditSummary.PERSISTENCE_HELPER);
    }

    @Override
    protected DBOpenHelper getDbOpenHelper() {
        return ((WikipediaApp)getContext().getApplicationContext()).getDbOpenHelper();
    }
}
