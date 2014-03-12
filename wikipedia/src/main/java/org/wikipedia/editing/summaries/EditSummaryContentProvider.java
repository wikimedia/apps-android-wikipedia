package org.wikipedia.editing.summaries;

import org.wikipedia.*;
import org.wikipedia.data.*;

public class EditSummaryContentProvider extends SQLiteContentProvider<EditSummary> {
    public EditSummaryContentProvider() {
        super(EditSummary.persistanceHelper);
    }

    @Override
    protected DBOpenHelper getDbOpenHelper() {
        return ((WikipediaApp)getContext().getApplicationContext()).getDbOpenHelper();
    }
}
