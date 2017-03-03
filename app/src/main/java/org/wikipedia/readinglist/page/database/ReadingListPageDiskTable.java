package org.wikipedia.readinglist.page.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.async.AsyncTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.readinglist.page.ReadingListPageRow;
import org.wikipedia.readinglist.page.database.disk.DiskRow;
import org.wikipedia.readinglist.page.database.disk.DiskStatus;
import org.wikipedia.readinglist.page.database.disk.ReadingListPageDiskRow;

public class ReadingListPageDiskTable
        extends AsyncTable<DiskStatus, ReadingListPageRow, DiskRow<ReadingListPageRow>> {
    private static final int DATABASE_VERSION = 12;

    public ReadingListPageDiskTable() {
        super(ReadingListPageContract.TABLE_DISK, ReadingListPageContract.Disk.URI,
                ReadingListPageContract.DISK_COLS);
    }

    @Override public ReadingListPageDiskRow fromCursor(@NonNull Cursor cursor) {
        DiskRow<ReadingListPageRow> diskRow = ReadingListPageContract.DISK_COLS.val(cursor);
        return new ReadingListPageDiskRow(diskRow, null);
    }

    @NonNull @Override public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case DATABASE_VERSION:
                Column<?>[] diskCols = super.getColumnsAdded(version);
                Column<?>[] cols = new Column<?>[diskCols.length + 1];
                System.arraycopy(diskCols, 0, cols, 0, diskCols.length);
                cols[diskCols.length] = ReadingListPageContract.DiskCol.FILENAME;
                return cols;
            default:
                return super.getColumnsAdded(version);
        }
    }

    @Override protected ContentValues toContentValues(@NonNull DiskRow<ReadingListPageRow> row) {
        return ReadingListPageContract.DISK_COLS.toContentValues(row);
    }

    @Override protected int getDBVersionIntroducedAt() {
        return DATABASE_VERSION;
    }
}
