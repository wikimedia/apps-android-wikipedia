package org.wikipedia.readinglist.page.database.disk;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.ReadingListPageRow;

public class ReadingListPageDiskRow extends ReadingListDiskRow {
    public static ReadingListPageDiskRow fromCursor(@NonNull Cursor cursor) {
        ReadingListDiskRow diskRow = ReadingListPage.DISK_DATABASE_TABLE.fromCursor(cursor);
        boolean hasRow = ReadingListPageContract.DiskWithPage.KEY.val(cursor) != null;
        ReadingListPageRow row = hasRow ? ReadingListPage.DATABASE_TABLE.fromCursor(cursor) : null;
        return new ReadingListPageDiskRow(diskRow, row);
    }

    public ReadingListPageDiskRow(@NonNull ReadingListPage row) {
        super(row.key(), row, null);
    }

    public ReadingListPageDiskRow(@NonNull ReadingListDiskRow diskRow,
                                  @Nullable ReadingListPageRow row) {
        this(diskRow, row, diskRow.filename());
    }

    public ReadingListPageDiskRow(@NonNull DiskRow<ReadingListPageRow> diskRow,
                                  @Nullable ReadingListPageRow dat,
                                  @Nullable String filename) {
        super(diskRow, dat, filename);
    }
}