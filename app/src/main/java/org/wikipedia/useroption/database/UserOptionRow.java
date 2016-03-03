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

    public UserOptionRow(@NonNull String key) {
        super(key);
        init();
    }

    public UserOptionRow(@NonNull UserOption option) {
        super(option);
        init();
    }

    public UserOptionRow(@NonNull String key, @Nullable String val) {
        super(key, val);
        init();
    }

    public UserOptionRow(@NonNull String key, @Nullable String val, @NonNull SyncStatus status,
                         long transactionId, long timestamp) {
        super(key, val);
        init(status, transactionId, timestamp);
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

    private void init() {
        init(null, 0, 0);
    }

    private void init(@Nullable SyncStatus status, long transactionId, long timestamp) {
        sync = status == null
                ? new DefaultSyncRow()
                : new DefaultSyncRow(status, transactionId, timestamp);
    }
}