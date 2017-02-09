package org.wikipedia.database.async;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.CodeEnumColumn;
import org.wikipedia.database.column.IdColumn;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.StrColumn;
import org.wikipedia.model.CodeEnum;
import org.wikipedia.model.EnumCode;

public abstract class AsyncColumns<Status extends EnumCode, Dat, Row extends AsyncRow<Status, Dat>> {
    @NonNull private final IdColumn id;

    /** A key used to uniquely identify a row and often to associate a row with a row in another
     * table. No constraints are made on referent keys except that they must be nonnull unique. */
    @NonNull private final StrColumn key;

    /** The status code indicating an outstanding transaction. */
    @NonNull private final CodeEnumColumn<Status> status;

    /** The timestamp for the last successful transaction in milliseconds or
     * {@link AsyncConstant#NO_TIMESTAMP} for never transacted. */
    @NonNull private final LongColumn timestamp;

    /** A unique identification for a transaction in progress or
     * {@link AsyncConstant#NO_TRANSACTION_ID} for not in progress. */
    @NonNull private final LongColumn transactionId;

    @NonNull private final String[] selection;

    @NonNull private final String[] content;

    public AsyncColumns(@NonNull String tbl, @NonNull String namePrefix,
                        @NonNull CodeEnum<Status> codeEnum) {
        id = new IdColumn(tbl);
        key = new StrColumn(tbl, namePrefix + "Key", "text not null unique");
        status = new CodeEnumColumn<>(tbl, namePrefix + "Status", codeEnum);
        timestamp = new LongColumn(tbl, namePrefix + "Timestamp", "integer not null");
        transactionId = new LongColumn(tbl, namePrefix + "TransactionId", "integer not null");
        selection = DbUtil.qualifiedNames(key);
        content = DbUtil.qualifiedNames(key, status, timestamp, transactionId);
    }

    @NonNull public IdColumn id() {
        return id;
    }

    @NonNull public StrColumn key() {
        return key;
    }

    @NonNull public CodeEnumColumn<Status> status() {
        return status;
    }

    @NonNull public LongColumn timestamp() {
        return timestamp;
    }

    @NonNull public LongColumn transactionId() {
        return transactionId;
    }

    @NonNull public String[] selection() {
        return selection;
    }

    @NonNull public String[] content() {
        return content;
    }

    @NonNull public String key(@NonNull Cursor cursor) {
        return key.val(cursor);
    }

    @NonNull public Status status(@NonNull Cursor cursor) {
        return status.val(cursor);
    }

    public long timestamp(@NonNull Cursor cursor) {
        return timestamp.val(cursor);
    }

    public long transactionId(@NonNull Cursor cursor) {
        return transactionId.val(cursor);
    }

    @NonNull public ContentValues toContentValues(@NonNull Row row) {
        ContentValues values = new ContentValues();
        values.put(key.getName(), row.key());
        values.put(status.getName(), row.statusCode());
        values.put(timestamp.getName(), row.timestamp());
        values.put(transactionId.getName(), row.transactionId());
        return values;
    }

    public abstract Row val(@NonNull Cursor cursor);
}
