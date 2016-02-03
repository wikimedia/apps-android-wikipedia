package org.wikipedia.editing.summaries;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseTable;

import java.util.Date;

public class EditSummaryDatabaseTable extends DatabaseTable<EditSummary> {

    private static final int DB_VER_INTRODUCED = 2;

    private static final String COL_SUMMARY = "summary";
    private static final String COL_LAST_USED = "lastUsed";

    public static final String[] SELECTION_KEYS = {
            COL_SUMMARY
    };

    @Override
    public EditSummary fromCursor(Cursor c) {
        String summary = c.getString(c.getColumnIndex(COL_SUMMARY));
        Date lastUsed = new Date(c.getLong(c.getColumnIndex(COL_LAST_USED)));
        return new EditSummary(summary, lastUsed);
    }

    @Override
    protected ContentValues toContentValues(EditSummary obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_SUMMARY, obj.getSummary());
        contentValues.put(COL_LAST_USED, obj.getLastUsed().getTime());
        return contentValues;
    }

    @Override
    public String getTableName() {
        return "editsummaries";
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
                        new Column(COL_SUMMARY, "string"),
                        new Column(COL_LAST_USED, "integer")
                };
            default:
                return new Column[0];
        }
    }

    @Override
    protected String getPrimaryKeySelection(@NonNull EditSummary obj, @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(obj, SELECTION_KEYS);
    }

    @Override
    protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull EditSummary obj) {
        return new String[] {
                obj.getSummary()
        };
    }
}
