package org.wikipedia.beta.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.wikipedia.beta.savedpages.SavedPage;
import org.wikipedia.beta.editing.summaries.EditSummary;
import org.wikipedia.beta.history.HistoryEntry;
import org.wikipedia.beta.pageimages.PageImage;

public class DBOpenHelper  extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "wikipedia.db";
    private static final int DATABASE_VERSION = 4;

    public DBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private PersistanceHelper[] persistanceHelpers = {
            HistoryEntry.PERSISTANCE_HELPER,
            PageImage.PERSISTANCE_HELPER,
            SavedPage.PERSISTANCE_HELPER,
            EditSummary.PERSISTANCE_HELPER
    };
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        for (PersistanceHelper ph : persistanceHelpers) {
            ph.createTables(sqLiteDatabase, DATABASE_VERSION);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int from, int to) {
        for (PersistanceHelper ph : persistanceHelpers) {
            ph.upgradeSchema(sqLiteDatabase, from, to);
        }
    }
}
