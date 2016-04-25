package org.wikipedia.readinglist.page;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.Validate;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.readinglist.page.database.disk.DiskStatus;
import org.wikipedia.readinglist.page.database.disk.ReadingListPageDiskRow;
import org.wikipedia.util.FileUtil;

public final class ReadingListPage extends ReadingListPageRow {
    @NonNull private DiskStatus diskStatus;
    @Nullable private String filename;

    public static ReadingListPage fromCursor(@NonNull Cursor cursor) {
        ReadingListPageDiskRow diskRow = ReadingListPage.DISK_DATABASE_TABLE.fromCursor(cursor);
        ReadingListPageRow row = ReadingListPage.DATABASE_TABLE.fromCursor(cursor);
        return builder()
                .copy(row)
                .diskStatus(diskRow.status())
                .filename(diskRow.filename())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @NonNull public DiskStatus diskStatus() {
        return diskStatus;
    }

    @Nullable public String filename() {
        return filename;
    }

    public boolean savedOrSaving() {
        return diskStatus.savedOrSaving();
    }

    public void savedOrSaving(boolean saved) {
        if (saved) {
            diskStatus = diskStatus == DiskStatus.SAVED ? DiskStatus.SAVED : DiskStatus.OUTDATED;
        } else {
            diskStatus = diskStatus == DiskStatus.ONLINE ? DiskStatus.ONLINE : DiskStatus.UNSAVED;
        }
    }

    private ReadingListPage(@NonNull Builder builder) {
        super(builder);
        diskStatus = builder.diskStatus;
        filename = builder.filename;
    }

    public static class Builder extends ReadingListPageRow.Builder<Builder> {
        private DiskStatus diskStatus;
        private String filename;

        public Builder copy(@NonNull ReadingListPage copy) {
            super.copy(copy);
            diskStatus = copy.diskStatus;
            filename = copy.filename;
            return this;
        }

        public Builder diskStatus(@NonNull DiskStatus diskStatus) {
            this.diskStatus = diskStatus;
            return this;
        }

        public Builder filename(@Nullable String filename) {
            this.filename = filename;
            return this;
        }

        @Override public ReadingListPage build() {
            validate();
            ReadingListPage page = new ReadingListPage(this);
            page.filename = FileUtil.getSavedPageDirFor(ReadingListDaoProxy.pageTitle(page));
            return page;
        }

        @Override protected void validate() {
            super.validate();
            Validate.notNull(diskStatus);
        }
    }
}