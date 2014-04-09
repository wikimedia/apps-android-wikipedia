package org.wikipedia.data;

import android.content.*;
import android.database.sqlite.*;
import org.wikipedia.editing.summaries.*;
import org.wikipedia.history.*;
import org.wikipedia.pageimages.*;
import org.wikipedia.savedpages.*;

public class DBOpenHelper  extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "wikipedia.db";
    private static final int DATABASE_VERSION = 2;

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
