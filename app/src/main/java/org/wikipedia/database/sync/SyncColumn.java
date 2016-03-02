package org.wikipedia.database.sync;

import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.column.Column;
import org.wikipedia.database.column.LongColumn;

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
    LongColumn SYNC_TRANSACTION_ID = new LongColumn("syncTransactionId", "integer not null");

    /** The timestamp for the last successful synchronization in milliseconds or 0 for never synchronized. */
    LongColumn SYNC_TIMESTAMP = new LongColumn("syncTimestamp", "integer not null");

    List<? extends Column<?>> ALL = Arrays.<Column<?>>asList(SYNC_STATUS, SYNC_TRANSACTION_ID, SYNC_TIMESTAMP);
}
