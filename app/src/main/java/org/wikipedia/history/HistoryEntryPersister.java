package org.wikipedia.history;

import android.content.Context;

import org.wikipedia.data.ContentPersister;

public class HistoryEntryPersister extends ContentPersister<HistoryEntry> {
    public HistoryEntryPersister(Context context) {
        super(context, HistoryEntry.PERSISTENCE_HELPER, HistoryEntry.PERSISTENCE_HELPER.getTableName());
    }
}