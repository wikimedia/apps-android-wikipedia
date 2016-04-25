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
        ReadingListPageDiskRow diskRow = ReadingListPage.DISK_DATABASE_TABLE.fromCursor(cursor);
        boolean hasRow = ReadingListPageContract.DiskWithPage.KEY.val(cursor) != null;
        ReadingListPageRow row = hasRow ? ReadingListPage.DATABASE_TABLE.fromCursor(cursor) : null;
        return new ReadingListPageDiskRow(diskRow, row);
    }

    public ReadingListPageDiskRow(@NonNull ReadingListPage dat) {
        this(dat.key(), dat, dat.filename());
    }

    public ReadingListPageDiskRow(@NonNull ReadingListPageDiskRow diskRow,
                                  @Nullable ReadingListPageRow dat) {
        this(diskRow, dat, diskRow.filename());
    }

    public ReadingListPageDiskRow(@NonNull String key, @Nullable ReadingListPageRow dat,
                                  @Nullable String filename) {
        super(key, dat);
        this.filename = filename;
    }

    public ReadingListPageDiskRow(@NonNull DiskRow<ReadingListPageRow> diskRow,
                                  @Nullable ReadingListPageRow dat,
                                  @Nullable String filename) {
        super(diskRow, dat);
        this.filename = filename;
    }

    @Nullable public String filename() {
        return filename;
    }
}
