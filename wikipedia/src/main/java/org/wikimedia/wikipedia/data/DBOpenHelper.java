package org.wikimedia.wikipedia.data;

import android.content.*;
import android.database.sqlite.*;
import org.wikimedia.wikipedia.history.HistoryEntry;

public class DBOpenHelper  extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "wikipedia.db";
    private static final int DATABASE_VERSION = 1;

    public DBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(HistoryEntry.persistanceHelper.getSchema(DATABASE_VERSION));
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int from, int to) {
    }
}
