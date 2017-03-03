package org.wikipedia.readinglist.page.database.disk;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.ReadingListPageRow;

public class ReadingListPageDiskRow extends DiskRow<ReadingListPageRow> {
    public static ReadingListPageDiskRow fromCursor(@NonNull Cursor cursor) {
        ReadingListPageDiskRow diskRow = ReadingListPage.DISK_DATABASE_TABLE.fromCursor(cursor);
        boolean hasRow = ReadingListPageContract.DiskWithPage.KEY.val(cursor) != null;
        ReadingListPageRow row = hasRow ? ReadingListPage.DATABASE_TABLE.fromCursor(cursor) : null;
        return new ReadingListPageDiskRow(diskRow, row);
    }

    public ReadingListPageDiskRow(@NonNull ReadingListPage dat) {
        this(dat.key(), dat);
    }

    public ReadingListPageDiskRow(@NonNull String key, @Nullable ReadingListPageRow dat) {
        super(key, dat);
    }

    public ReadingListPageDiskRow(@NonNull DiskRow<ReadingListPageRow> diskRow,
                                  @Nullable ReadingListPageRow dat) {
        super(diskRow, dat);
    }
}
