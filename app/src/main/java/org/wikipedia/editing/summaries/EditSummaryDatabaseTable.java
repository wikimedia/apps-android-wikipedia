package org.wikipedia.editing.summaries;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.StrColumn;

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
        String summary = getString(c, COL_SUMMARY);
        Date lastUsed = new Date(getLong(c, COL_LAST_USED));
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
    public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case DB_VER_INTRODUCED:
                return new Column<?>[] {
                        new LongColumn("_id", "integer primary key"),
                        new StrColumn(COL_SUMMARY, "string"),
                        new LongColumn(COL_LAST_USED, "integer")
                };
            default:
                return super.getColumnsAdded(version);
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
