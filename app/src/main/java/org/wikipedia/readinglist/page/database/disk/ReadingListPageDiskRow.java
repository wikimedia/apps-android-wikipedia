package org.wikipedia.readinglist.page.database.disk;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.ReadingListPageRow;

public class ReadingListPageDiskRow extends DiskRow<ReadingListPageRow> {
    @Nullable private final String filename;

    public static ReadingListPageDiskRow fromCursor(@NonNull Cursor cursor) {
        ReadingListDiskRow diskRow = ReadingListPage.DISK_DATABASE_TABLE.fromCursor(cursor);
        boolean hasRow = ReadingListPageContract.DiskWithPage.KEY.val(cursor) != null;
        ReadingListPageRow row = hasRow ? ReadingListPage.DATABASE_TABLE.fromCursor(cursor) : null;
        return new ReadingListPageDiskRow(diskRow, row);
    }

    public ReadingListPageDiskRow(@NonNull ReadingListPage row) {
        super(row.key(), row);
        filename = null;
    }

    public ReadingListPageDiskRow(@NonNull ReadingListDiskRow diskRow,
                                  @Nullable ReadingListPageRow row) {
        super(diskRow, row);
        filename = diskRow.filename();
    }

    @Nullable public String filename() {
        return filename;
    }
}
