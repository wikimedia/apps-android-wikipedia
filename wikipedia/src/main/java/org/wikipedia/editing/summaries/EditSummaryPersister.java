package org.wikipedia.editing.summaries;

import android.content.*;
import org.wikipedia.data.*;

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
