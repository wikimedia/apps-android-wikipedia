package org.wikipedia.search;

import org.wikipedia.data.PersistenceHelper;

import android.content.ContentValues;
import android.database.Cursor;
import java.util.Date;

public class RecentSearchPersistenceHelper extends PersistenceHelper<RecentSearch> {

    private static final int DB_VER_INTRODUCED = 5;

    private static final int COL_INDEX_TEXT = 1;
    private static final int COL_INDEX_TIMESTAMP = 2;

    @Override
    public RecentSearch fromCursor(Cursor c) {
        // Carefully, get them back by using position only
        String title = c.getString(COL_INDEX_TEXT);
        Date timestamp = new Date(c.getLong(COL_INDEX_TIMESTAMP));
        return new RecentSearch(title, timestamp);
    }

    @Override
    protected ContentValues toContentValues(RecentSearch obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("text", obj.getText());
        contentValues.put("timestamp", obj.getTimestamp().getTime());
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
                        new Column("text", "string"),
                        new Column("timestamp", "integer"),
                };
            default:
                return new Column[0];
        }
    }

    @Override
    protected String getPrimaryKeySelection() {
        return "text = ?";
    }

    @Override
    protected String[] getPrimaryKeySelectionArgs(RecentSearch obj) {
        return new String[] {
                obj.getText(),
        };
    }
}
