package org.wikipedia.editing.summaries;

import android.content.Context;
import org.wikipedia.data.ContentPersister;

public class EditSummaryPersister extends ContentPersister<EditSummary> {
    public EditSummaryPersister(Context context) {
        // lolJava
        super(
                context.getContentResolver().acquireContentProviderClient(
                        EditSummary.PERSISTENCE_HELPER.getBaseContentURI()
                ),
                EditSummary.PERSISTENCE_HELPER
        );
    }
}
