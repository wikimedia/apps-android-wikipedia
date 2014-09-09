package org.wikipedia.beta.editing.summaries;

import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.data.DBOpenHelper;
import org.wikipedia.beta.data.SQLiteContentProvider;

public class EditSummaryContentProvider extends SQLiteContentProvider<EditSummary> {
    public EditSummaryContentProvider() {
        super(EditSummary.PERSISTANCE_HELPER);
    }

    @Override
    protected DBOpenHelper getDbOpenHelper() {
        return ((WikipediaApp)getContext().getApplicationContext()).getDbOpenHelper();
    }
}
