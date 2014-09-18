package org.wikipedia.beta.history;

import android.content.Context;
import org.wikipedia.beta.data.ContentPersister;
import org.wikipedia.beta.data.SQLiteContentProvider;

public class HistoryEntryPersister extends ContentPersister<HistoryEntry> {
    public HistoryEntryPersister(Context context) {
        // lolJava
        super(
                context.getContentResolver().acquireContentProviderClient(
                        SQLiteContentProvider.getAuthorityForTable(
                                HistoryEntry.PERSISTANCE_HELPER.getTableName()
                        )
                ),
                HistoryEntry.PERSISTANCE_HELPER
        );
    }
}
