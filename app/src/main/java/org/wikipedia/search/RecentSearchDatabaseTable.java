package org.wikipedia.search;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseTable;

import java.util.Date;

public class RecentSearchDatabaseTable extends DatabaseTable<RecentSearch> {

    private static final int DB_VER_INTRODUCED = 5;

    private static final String COL_TEXT = "text";
    private static final String COL_TIMESTAMP = "timestamp";

    public static final String[] SELECTION_KEYS = {
            COL_TEXT
    };

    @Override
    public RecentSearch fromCursor(Cursor c) {
        String title = c.getString(c.getColumnIndex(COL_TEXT));
        Date timestamp = new Date(c.getLong(c.getColumnIndex(COL_TIMESTAMP)));
        return new RecentSearch(title, timestamp);
    }

    @Override
    protected ContentValues toContentValues(RecentSearch obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_TEXT, obj.getText());
        contentValues.put(COL_TIMESTAMP, obj.getTimestamp().getTime());
        return contentValues;
    }

    @Override
    public String getTableName() {
        return "recentsearches";
    }

    @Override
    protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
    }

    @Override
    public Column[] getColumnsAdded(int version) {
        switch (version) {
            case DB_VER_INTRODUCED:
                return new Column[] {
                        new Column("_id", "integer primary key"),
                        new Column(COL_TEXT, "string"),
                        new Column(COL_TIMESTAMP, "integer"),
                };
            default:
                return new Column[0];
        }
    }

    @Override
    protected String getPrimaryKeySelection(@NonNull RecentSearch obj, @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(obj, SELECTION_KEYS);
    }

    @Override
    protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull RecentSearch obj) {
        return new String[] {
                obj.getText(),
        };
    }
}
