package org.wikipedia.database.sync;

import android.support.annotation.NonNull;

public interface SyncRow {
    @NonNull SyncStatus status();
    long transactionId();
    long timestamp();

    void resetTransaction(@NonNull SyncStatus status);
    void startTransaction();
    boolean isTransaction(@NonNull SyncRow row);
    void completeTransaction(long timestamp);
}