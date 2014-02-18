package org.wikipedia.history;

import android.content.*;
import org.wikipedia.data.*;

public class HistoryEntryPersister extends ContentPersister<HistoryEntry> {
    public HistoryEntryPersister(Context context) {
        // lolJava
        super(
                context.getContentResolver().acquireContentProviderClient(
                        SQLiteContentProvider.getAuthorityForTable(
                                HistoryEntry.persistanceHelper.getTableName()
                        )
                ),
                HistoryEntry.persistanceHelper
        );
    }
}
