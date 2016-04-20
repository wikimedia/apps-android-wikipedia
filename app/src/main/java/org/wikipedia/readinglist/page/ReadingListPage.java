package org.wikipedia.readinglist.page;

import android.database.Cursor;
import android.support.annotation.NonNull;

import org.apache.commons.lang3.Validate;
import org.wikipedia.readinglist.page.database.disk.DiskStatus;
import org.wikipedia.readinglist.page.database.disk.ReadingListDiskRow;

public final class ReadingListPage extends ReadingListPageRow {
    @NonNull private final DiskStatus diskStatus;

    public static ReadingListPage fromCursor(@NonNull Cursor cursor) {
        ReadingListDiskRow diskRow = ReadingListPage.DISK_DATABASE_TABLE.fromCursor(cursor);
        ReadingListPageRow row = ReadingListPage.DATABASE_TABLE.fromCursor(cursor);
        return builder()
                .copy(row)
                .diskStatus(diskRow.status())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @NonNull public DiskStatus diskStatus() {
        return diskStatus;
    }

    public boolean savedOrSaving() {
        return diskStatus.savedOrSaving();
    }

    private ReadingListPage(@NonNull Builder builder) {
        super(builder);
        diskStatus = builder.diskStatus;
    }

    public static class Builder extends ReadingListPageRow.Builder<Builder> {
        private DiskStatus diskStatus;

        public Builder copy(@NonNull ReadingListPage copy) {
            super.copy(copy);
            diskStatus = copy.diskStatus;
            return this;
        }

        public Builder diskStatus(@NonNull DiskStatus diskStatus) {
            this.diskStatus = diskStatus;
            return this;
        }

        @Override public ReadingListPage build() {
            validate();
            return new ReadingListPage(this);
        }

        @Override protected void validate() {
            super.validate();
            Validate.notNull(diskStatus);
        }
    }
}
