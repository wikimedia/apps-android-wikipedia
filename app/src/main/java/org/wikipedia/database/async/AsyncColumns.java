package org.wikipedia.database.async;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.column.Column;
import org.wikipedia.database.column.LongColumn;

import java.util.Arrays;
import java.util.List;

public abstract class AsyncColumns<T> {
    /** The code indicating an outstanding transaction. */
    @NonNull private final Column<T> status;

    /** The timestamp for the last successful transaction in milliseconds or 0 for transacted. */
    @NonNull private final LongColumn timestamp;

    /** A unique identification for a transaction in progress or 0 for not in progress. */
    @NonNull private final LongColumn transactionId;

    protected AsyncColumns(@NonNull String namePrefix) {
        status = new Column<T>(namePrefix + "Status", "integer not null") {
            @Override
            public T val(@NonNull Cursor cursor) {
                return statusOf(getInt(cursor));
            }
        };
        timestamp = new LongColumn(namePrefix + "Timestamp", "integer not null");
        transactionId = new LongColumn(namePrefix + "TransactionId", "integer not null");
    }

    @NonNull public List<? extends Column<?>> all() {
        return Arrays.asList(status, timestamp, transactionId);
    }

    @NonNull public String status() {
        return status.getName();
    }

    @NonNull public String timestamp() {
        return timestamp.getName();
    }

    @NonNull public String transactionId() {
        return transactionId.getName();
    }

    @NonNull public T status(@NonNull Cursor cursor) {
        return status.val(cursor);
    }

    public long timestamp(@NonNull Cursor cursor) {
        return timestamp.val(cursor);
    }

    public long transactionId(@NonNull Cursor cursor) {
        return transactionId.val(cursor);
    }

    public void put(@NonNull ContentValues values, AsyncRow<T> row) {
        values.put(status.getName(), row.statusCode());
        values.put(timestamp.getName(), row.timestamp());
        values.put(transactionId.getName(), row.transactionId());
    }

    @NonNull public abstract AsyncRow<T> val(@NonNull Cursor cursor);

    @NonNull protected abstract T statusOf(int code);
}