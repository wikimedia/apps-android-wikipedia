package org.wikipedia.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.wikipedia.savedpages.SavedPage;
import org.wikipedia.editing.summaries.EditSummary;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.search.RecentSearch;

public class DBOpenHelper  extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "wikipedia.db";
    private static final int DATABASE_VERSION = 5;

    public DBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private PersistenceHelper[] persistenceHelpers = {
            HistoryEntry.PERSISTENCE_HELPER,
            PageImage.PERSISTENCE_HELPER,
            RecentSearch.PERSISTENCE_HELPER,
            SavedPage.PERSISTENCE_HELPER,
            EditSummary.PERSISTENCE_HELPER
    };
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        for (PersistenceHelper ph : persistenceHelpers) {
            ph.createTables(sqLiteDatabase, DATABASE_VERSION);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int from, int to) {
        for (PersistenceHelper ph : persistenceHelpers) {
            ph.upgradeSchema(sqLiteDatabase, from, to);
        }
    }
}
