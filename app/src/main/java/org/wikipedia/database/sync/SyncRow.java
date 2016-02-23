package org.wikipedia.database.sync;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface SyncRow {
    @NonNull SyncStatus status();
    long transactionId();
    long timestamp();

    void resetTransaction(@NonNull SyncStatus status);
    void startTransaction();
    boolean completeable(@Nullable SyncRow old);
    void completeTransaction(long timestamp);
}