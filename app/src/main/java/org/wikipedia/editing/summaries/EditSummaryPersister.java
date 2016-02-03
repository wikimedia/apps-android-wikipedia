package org.wikipedia.editing.summaries;

import android.content.Context;

import org.wikipedia.data.ContentPersister;

public class EditSummaryPersister extends ContentPersister<EditSummary> {
    public EditSummaryPersister(Context context) {
        super(context, EditSummary.PERSISTENCE_HELPER, EditSummary.PERSISTENCE_HELPER.getBaseContentURI());
    }
}