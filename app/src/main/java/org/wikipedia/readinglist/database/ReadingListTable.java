package org.wikipedia.readinglist.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.contract.ReadingListContract;

import java.util.ArrayList;
import java.util.List;

public class ReadingListTable extends DatabaseTable<ReadingListRow> {
    private static final int DB_VER_INTRODUCED = 13;

    public ReadingListTable() {
        super(ReadingListContract.TABLE, ReadingListContract.List.URI);
    }

    @Override public ReadingListRow fromCursor(@NonNull Cursor cursor) {
        return ReadingListRow
                .builder()
                .key(ReadingListContract.List.KEY.val(cursor))
                .title(ReadingListContract.List.TITLE.val(cursor))
                .mtime(ReadingListContract.List.MTIME.val(cursor))
                .atime(ReadingListContract.List.ATIME.val(cursor))
                .description(ReadingListContract.List.DESCRIPTION.val(cursor))
                .build();
    }

    @NonNull @Override public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case DB_VER_INTRODUCED:
                List<Column<?>> cols = new ArrayList<>();
                cols.add(ReadingListContract.List.ID);
                cols.add(ReadingListContract.List.KEY);
                cols.add(ReadingListContract.List.TITLE);
                cols.add(ReadingListContract.List.MTIME);
                cols.add(ReadingListContract.List.ATIME);
                cols.add(ReadingListContract.List.DESCRIPTION);
                return cols.toArray(new Column<?>[cols.size()]);
            default:
                return super.getColumnsAdded(version);
        }
    }

    @Override protected ContentValues toContentValues(@NonNull ReadingListRow row) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadingListContract.List.KEY.getName(), row.key());
        contentValues.put(ReadingListContract.List.TITLE.getName(), row.getTitle());
        contentValues.put(ReadingListContract.List.MTIME.getName(), row.mtime());
        contentValues.put(ReadingListContract.List.ATIME.getName(), row.atime());
        contentValues.put(ReadingListContract.List.DESCRIPTION.getName(), row.getDescription());
        return contentValues;
    }

    @Override protected String getPrimaryKeySelection(@NonNull ReadingListRow row,
                                                      @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(row, ReadingListContract.List.SELECTION);
    }

    @Override protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull ReadingListRow row) {
        return new String[] {row.key()};
    }

    @Override protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
    }
}