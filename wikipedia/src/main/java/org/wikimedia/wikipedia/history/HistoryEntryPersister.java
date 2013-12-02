package org.wikimedia.wikipedia.history;

import android.content.ContentProviderClient;
import android.content.Context;
import org.wikimedia.wikipedia.data.ContentPersister;
import org.wikimedia.wikipedia.data.PersistanceHelper;
import org.wikimedia.wikipedia.data.SQLiteContentProvider;

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
