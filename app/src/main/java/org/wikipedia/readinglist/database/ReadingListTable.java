package org.wikipedia.readinglist.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.contract.ReadingListContract;

import java.util.ArrayList;
import java.util.List;

public class ReadingListTable extends DatabaseTable<ReadingList> {
    private static final int DB_VER_INTRODUCED = 18;

    public ReadingListTable() {
        super(ReadingListContract.TABLE, ReadingListContract.URI);
    }

    @Override public ReadingList fromCursor(@NonNull Cursor cursor) {
        ReadingList list = new ReadingList(ReadingListContract.Col.TITLE.val(cursor),
                ReadingListContract.Col.DESCRIPTION.val(cursor));
        list.id(ReadingListContract.Col.ID.val(cursor));
        list.atime(ReadingListContract.Col.ATIME.val(cursor));
        list.mtime(ReadingListContract.Col.MTIME.val(cursor));
        list.sizeBytes(ReadingListContract.Col.SIZEBYTES.val(cursor));
        list.dirty(ReadingListContract.Col.DIRTY.val(cursor) != 0);
        list.remoteId(ReadingListContract.Col.REMOTEID.val(cursor));
        return list;
    }

    @NonNull @Override public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case DB_VER_INTRODUCED:
                List<Column<?>> cols = new ArrayList<>();
                cols.add(ReadingListContract.Col.ID);
                cols.add(ReadingListContract.Col.TITLE);
                cols.add(ReadingListContract.Col.MTIME);
                cols.add(ReadingListContract.Col.ATIME);
                cols.add(ReadingListContract.Col.DESCRIPTION);
                cols.add(ReadingListContract.Col.SIZEBYTES);
                cols.add(ReadingListContract.Col.DIRTY);
                cols.add(ReadingListContract.Col.REMOTEID);
                return cols.toArray(new Column<?>[cols.size()]);
            default:
                return super.getColumnsAdded(version);
        }
    }

    @Override protected ContentValues toContentValues(@NonNull ReadingList row) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadingListContract.Col.TITLE.getName(), row.dbTitle());
        contentValues.put(ReadingListContract.Col.MTIME.getName(), row.mtime());
        contentValues.put(ReadingListContract.Col.ATIME.getName(), row.atime());
        contentValues.put(ReadingListContract.Col.DESCRIPTION.getName(), row.description());
        contentValues.put(ReadingListContract.Col.SIZEBYTES.getName(), row.sizeBytes());
        contentValues.put(ReadingListContract.Col.DIRTY.getName(), row.dirty() ? 1 : 0);
        contentValues.put(ReadingListContract.Col.REMOTEID.getName(), row.remoteId());
        return contentValues;
    }

    @Override protected String getPrimaryKeySelection(@NonNull ReadingList row,
                                                      @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(row, ReadingListContract.Col.SELECTION);
    }

    @Override protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull ReadingList row) {
        return new String[] {row.dbTitle()};
    }

    @Override protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
    }
}
