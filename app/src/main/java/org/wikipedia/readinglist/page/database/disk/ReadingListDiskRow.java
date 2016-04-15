package org.wikipedia.readinglist.page.database.disk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.readinglist.page.ReadingListPageRow;

// TODO: rename to ReadingListPageDiskRow. I'm not sure what to call the current
//       ReadingListPageDiskRow which contains an optional page model in addition to this class.
public class ReadingListDiskRow extends DiskRow<ReadingListPageRow> {
    @Nullable private final String filename;

    public ReadingListDiskRow(@NonNull DiskRow<ReadingListPageRow> diskRow,
                              @Nullable ReadingListPageRow dat,
                              @Nullable String filename) {
        super(diskRow, dat);
        this.filename = filename;
    }

    @Nullable public String filename() {
        return filename;
    }
}