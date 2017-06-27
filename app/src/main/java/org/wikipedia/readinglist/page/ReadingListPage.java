package org.wikipedia.readinglist.page;

import android.database.Cursor;
import android.support.annotation.NonNull;

import org.apache.commons.lang3.Validate;
import org.wikipedia.readinglist.page.database.disk.DiskStatus;
import org.wikipedia.readinglist.page.database.disk.ReadingListPageDiskRow;

public final class ReadingListPage extends ReadingListPageRow {
    @NonNull private DiskStatus diskStatus;
    private boolean selected;

    public static ReadingListPage fromCursor(@NonNull Cursor cursor) {
        ReadingListPageDiskRow diskRow = ReadingListPage.DISK_DATABASE_TABLE.fromCursor(cursor);
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

    public boolean isOffline() {
        return diskStatus.savedOrSaving();
    }

    public boolean isSaving() {
        return diskStatus.saving();
    }

    public void setOffline(boolean offline) {
        if (offline) {
            diskStatus = diskStatus == DiskStatus.SAVED ? DiskStatus.SAVED : DiskStatus.OUTDATED;
        } else {
            diskStatus = diskStatus == DiskStatus.ONLINE ? DiskStatus.ONLINE : DiskStatus.UNSAVED;
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
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
