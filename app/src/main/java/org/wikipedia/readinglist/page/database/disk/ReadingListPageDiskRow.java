package org.wikipedia.readinglist.page.database.disk;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.ReadingListPageRow;

public class ReadingListPageDiskRow extends DiskRow<ReadingListPageRow> {
    public static ReadingListPageDiskRow fromCursor(@NonNull Cursor cursor) {
        DiskRow<ReadingListPageRow> diskRow = ReadingListPage.DISK_DATABASE_TABLE.fromCursor(cursor);
        boolean hasRow = cursor.getColumnIndex(ReadingListPageContract.DiskWithPage.KEY.getName()) != -1;
        ReadingListPageRow row = hasRow ? ReadingListPage.DATABASE_TABLE.fromCursor(cursor) : null;
        return new ReadingListPageDiskRow(diskRow, row);
    }

    public ReadingListPageDiskRow(@NonNull ReadingListPage row) {
        super(row.key(), row);
    }

    public ReadingListPageDiskRow(@NonNull DiskRow<ReadingListPageRow> diskRow,
                                  @Nullable ReadingListPageRow row) {
        super(diskRow, row);
    }
}