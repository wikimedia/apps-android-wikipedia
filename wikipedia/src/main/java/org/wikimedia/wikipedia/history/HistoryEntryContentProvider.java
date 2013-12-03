package org.wikimedia.wikipedia.history;

import org.wikimedia.wikipedia.WikipediaApp;
import org.wikimedia.wikipedia.data.DBOpenHelper;
import org.wikimedia.wikipedia.data.SQLiteContentProvider;

public class HistoryEntryContentProvider extends SQLiteContentProvider<HistoryEntry> {
    public HistoryEntryContentProvider() {
        super(HistoryEntry.persistanceHelper);
    }

    @Override
    protected DBOpenHelper getDbOpenHelper() {
        return ((WikipediaApp)getContext().getApplicationContext()).getDbOpenHelper();
    }
}
