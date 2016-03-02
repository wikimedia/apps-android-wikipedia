package org.wikipedia.useroption.database;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.sync.DefaultSyncRow;
import org.wikipedia.database.sync.SyncRow;
import org.wikipedia.database.sync.SyncStatus;
import org.wikipedia.useroption.UserOption;

public class UserOptionRow extends UserOption implements SyncRow {
    public static final UserOptionDatabaseTable DATABASE_TABLE = new UserOptionDatabaseTable();

    private SyncRow sync;

    public UserOptionRow(@NonNull String key, @Nullable String val, @NonNull SyncStatus status,
                         long transactionId, long timestamp) {
        super(key, val);
        sync = new DefaultSyncRow(status, transactionId, timestamp);
    }

    @NonNull
    @Override
    public SyncStatus status() {
        return sync.status();
    }

    @Override
    public long transactionId() {
        return sync.transactionId();
    }

    @Override
    public long timestamp() {
        return sync.timestamp();
    }

    @Override
    public void resetTransaction(@NonNull SyncStatus status) {
        sync.resetTransaction(status);
    }

    @Override
    public void startTransaction() {
        sync.startTransaction();
    }

    @Override
    public boolean isTransaction(@NonNull SyncRow row) {
        return sync.isTransaction(row);
    }

    @Override
    public void completeTransaction(long timestamp) {
        sync.completeTransaction(timestamp);
    }
}