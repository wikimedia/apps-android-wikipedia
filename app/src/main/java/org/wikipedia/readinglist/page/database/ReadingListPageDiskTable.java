package org.wikipedia.readinglist.page.database;

import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.async.AsyncTable;
import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.readinglist.page.ReadingListPageRow;
import org.wikipedia.readinglist.page.database.disk.DiskRow;
import org.wikipedia.readinglist.page.database.disk.DiskStatus;

public class ReadingListPageDiskTable
        extends AsyncTable<DiskStatus, ReadingListPageRow, DiskRow<ReadingListPageRow>> {
    private static final int DATABASE_VERSION = 12;

    public ReadingListPageDiskTable() {
        super(ReadingListPageContract.TABLE_DISK, ReadingListPageContract.Disk.URI, ReadingListPageContract.DISK_COLS);
    }

    @Override public DiskRow<ReadingListPageRow> fromCursor(@NonNull Cursor cursor) {
        return ReadingListPageContract.DISK_COLS.val(cursor);
    }

    @Override protected int getDBVersionIntroducedAt() {
        return DATABASE_VERSION;
    }
}