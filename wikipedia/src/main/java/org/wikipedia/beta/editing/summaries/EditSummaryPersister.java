package org.wikipedia.beta.editing.summaries;

import android.content.Context;
import org.wikipedia.beta.data.ContentPersister;

public class EditSummaryPersister extends ContentPersister<EditSummary> {
    public EditSummaryPersister(Context context) {
        // lolJava
        super(
                context.getContentResolver().acquireContentProviderClient(
                        EditSummary.PERSISTANCE_HELPER.getBaseContentURI()
                ),
                EditSummary.PERSISTANCE_HELPER
        );
    }
}
