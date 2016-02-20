package org.wikipedia.database.sync;

import android.support.annotation.NonNull;

public class DefaultSyncRow implements SyncRow {
    public static final int NO_TRANSACTION_ID = 0;

    @NonNull private SyncStatus status;
    private long transactionId;
    private long timestamp;

    public DefaultSyncRow() {
        this(SyncStatus.SYNCHRONIZED, NO_TRANSACTION_ID, 0);
    }

    public DefaultSyncRow(@NonNull SyncStatus status, long transactionId, long timestamp) {
        this.status = status;
        this.transactionId = transactionId;
        this.timestamp = timestamp;
    }

    @Override
    @NonNull
    public SyncStatus status() {
        return status;
    }

    @Override
    public long transactionId() {
        return transactionId;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public void resetTransaction(@NonNull SyncStatus status) {
        this.status = status;
        this.transactionId = NO_TRANSACTION_ID;
    }

    @Override
    public void startTransaction() {
        transactionId = newTransactionId();
    }

    @Override
    public boolean isTransaction(@NonNull SyncRow row) {
        return transactionId() == row.transactionId();
    }

    @Override
    public void completeTransaction(long timestamp) {
        this.timestamp = timestamp;
        resetTransaction(SyncStatus.SYNCHRONIZED);
    }

    private long newTransactionId() {
        return System.nanoTime();
    }
}