package org.wikipedia.useroption.database;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.async.AsyncRow;
import org.wikipedia.database.http.HttpRow;
import org.wikipedia.database.http.HttpStatus;
import org.wikipedia.useroption.UserOption;

public class UserOptionRow extends UserOption implements AsyncRow<HttpStatus> {
    public static final UserOptionDatabaseTable DATABASE_TABLE = new UserOptionDatabaseTable();

    private HttpRow http;

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
                         long timestamp, long transactionId) {
        super(key, val);
        init(status, timestamp, transactionId);
    }

    @NonNull @Override public HttpStatus status() {
        return http.status();
    }

    @Override
    public int statusCode() {
        return http.statusCode();
    }

    @Override
    public long timestamp() {
        return http.timestamp();
    }

    @Override
    public long transactionId() {
        return http.transactionId();
    }

    @Override
    public void resetTransaction(@NonNull HttpStatus status) {
        http.resetTransaction(status);
    }

    @Override
    public void startTransaction() {
        http.startTransaction();
    }

    @Override
    public boolean completeable(@Nullable AsyncRow<HttpStatus> old) {
        return http.completeable(old);
    }

    @Override
    public void completeTransaction(long timestamp) {
        http.completeTransaction(timestamp);
    }

    private void init() {
        init(null, HttpRow.NO_TIMESTAMP, HttpRow.NO_TRANSACTION_ID);
    }

    private void init(@Nullable HttpStatus status, long timestamp, long transactionId) {
        http = status == null
                ? new HttpRow()
                : new HttpRow(status, timestamp, transactionId);
    }
}
