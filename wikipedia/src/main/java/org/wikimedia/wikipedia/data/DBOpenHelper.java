package org.wikimedia.wikipedia.data;

import android.content.*;
import android.database.sqlite.*;
import org.wikimedia.wikipedia.history.HistoryEntry;
import org.wikimedia.wikipedia.pageimages.PageImage;
import org.wikimedia.wikipedia.savedpages.SavedPage;

public class DBOpenHelper  extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "wikipedia.db";
    private static final int DATABASE_VERSION = 1;

    public DBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(HistoryEntry.persistanceHelper.getSchema(DATABASE_VERSION));
        sqLiteDatabase.execSQL(PageImage.persistanceHelper.getSchema(DATABASE_VERSION));
        sqLiteDatabase.execSQL(SavedPage.persistanceHelper.getSchema(DATABASE_VERSION));
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int from, int to) {
    }
}
