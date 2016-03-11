package org.wikipedia.useroption.database;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.http.DefaultHttpRow;
import org.wikipedia.database.http.HttpRow;
import org.wikipedia.database.http.HttpStatus;
import org.wikipedia.useroption.UserOption;

public class UserOptionRow extends UserOption implements HttpRow {
    public static final UserOptionDatabaseTable DATABASE_TABLE = new UserOptionDatabaseTable();

    private HttpRow sync;

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

    public UserOptionRow(@NonNull String key, @Nullable String val, @NonNull HttpStatus status,
                         long transactionId, long timestamp) {
        super(key, val);
        init(status, transactionId, timestamp);
    }

    @NonNull
    @Override
    public HttpStatus status() {
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
    public void resetTransaction(@NonNull HttpStatus status) {
        sync.resetTransaction(status);
    }

    @Override
    public void startTransaction() {
        sync.startTransaction();
    }

    @Override
    public boolean completeable(@NonNull HttpRow row) {
        return sync.completeable(row);
    }

    @Override
    public void completeTransaction(long timestamp) {
        sync.completeTransaction(timestamp);
    }

    private void init() {
        init(null, 0, 0);
    }

    private void init(@Nullable HttpStatus status, long transactionId, long timestamp) {
        sync = status == null
                ? new DefaultHttpRow()
                : new DefaultHttpRow(status, transactionId, timestamp);
    }
}