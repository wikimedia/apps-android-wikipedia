package org.wikipedia.database.sync;

import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseTable.Column;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("checkstyle:interfaceistype")
public interface SyncColumn {
    /** The {@link SyncStatus} code indicating pending synchronization. */
    Column<SyncStatus> SYNC_STATUS = new Column<SyncStatus>("syncStatus", "integer not null") {
        @Override
        public SyncStatus val(@NonNull Cursor cursor) {
            return SyncStatus.of(getInt(cursor));
        }
    };

    /** A unique identification for a synchronization in progress or 0 for not in progress. */
    Column<Long> SYNC_TRANSACTION_ID = new Column<Long>("syncTransactionId", "integer not null") {
        @Override
        public Long val(@NonNull Cursor cursor) {
            return getLong(cursor);
        }
    };

    /** The timestamp for the last successful synchronization in milliseconds or 0 for never synchronized. */
    Column<Long> SYNC_TIMESTAMP = new Column<Long>("syncTimestamp", "integer not null") {
        @Override
        public Long val(@NonNull Cursor cursor) {
            return getLong(cursor);
        }
    };

    List<? extends Column> ALL = Arrays.asList(SYNC_STATUS, SYNC_TRANSACTION_ID, SYNC_TIMESTAMP);
}