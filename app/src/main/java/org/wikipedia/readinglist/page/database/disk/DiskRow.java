package org.wikipedia.readinglist.page.database.disk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.async.AsyncRow;

public class DiskRow<T> extends AsyncRow<DiskStatus, T> {
    private static final DiskStatus DEFAULT_STATUS = DiskStatus.ONLINE;

    public DiskRow(@NonNull String key, @Nullable T dat) {
        super(key, DEFAULT_STATUS, dat);
    }

    public DiskRow(@NonNull DiskRow<T> diskRow, @Nullable T dat) {
        super(diskRow, dat);
    }

    public DiskRow(@NonNull String key, @NonNull DiskStatus status, long timestamp,
                   long transactionId) {
        super(key, status, timestamp, transactionId);
    }

    @Override public boolean completable(@Nullable AsyncRow<DiskStatus, T> query) {
        boolean recordable = !(query == null && (status() == DiskStatus.DELETED));
        return super.completable(query) && recordable;
    }

    @Override public void completeTransaction(long timestamp) {
        super.completeTransaction(timestamp);
        resetTransaction(next(status()));
    }

    @NonNull private DiskStatus next(@NonNull DiskStatus current) {
        switch (current) {
            case ONLINE:
            case SAVED:
            case UNSAVED:
                return DiskStatus.ONLINE;
            case OUTDATED:
                return DiskStatus.SAVED;
            case DELETED:
                return DiskStatus.DELETED;
            default:
                throw new RuntimeException("current=" + current);
        }
    }
}
