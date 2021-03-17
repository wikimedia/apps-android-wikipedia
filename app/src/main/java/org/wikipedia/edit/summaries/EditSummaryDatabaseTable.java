package org.wikipedia.edit.summaries;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;

import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.contract.EditHistoryContract;
import org.wikipedia.database.contract.EditHistoryContract.Col;

import java.util.Date;

public class EditSummaryDatabaseTable extends DatabaseTable<EditSummary> {
    private static final int DB_VER_INTRODUCED = 2;


    public EditSummaryDatabaseTable() {
        super(EditHistoryContract.TABLE, EditHistoryContract.Summary.URI);
    }

    @Override
    public EditSummary fromCursor(Cursor cursor) {
        String summary = Col.SUMMARY.value(cursor);
        Date lastUsed = Col.LAST_USED.value(cursor);
        return new EditSummary(summary, lastUsed);
    }

    @Override
    protected ContentValues toContentValues(EditSummary obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Col.SUMMARY.getName(), obj.getSummary());
        contentValues.put(Col.LAST_USED.getName(), obj.getLastUsed().getTime());
        return contentValues;
    }

    @Override
    protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
    }

    @NonNull
    @Override
    public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case DB_VER_INTRODUCED:
                return new Column<?>[] {Col.ID, Col.SUMMARY, Col.LAST_USED};
            default:
                return super.getColumnsAdded(version);
        }
    }

    @Override
    protected String getPrimaryKeySelection(@NonNull EditSummary obj, @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(obj, EditHistoryContract.Summary.SELECTION);
    }

    @Override
    protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull EditSummary obj) {
        return new String[] {obj.getSummary()};
    }
}
