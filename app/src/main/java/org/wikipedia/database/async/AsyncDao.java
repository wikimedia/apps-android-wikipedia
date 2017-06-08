package org.wikipedia.database.async;

import android.support.annotation.NonNull;

import org.wikipedia.database.BaseDao;
import org.wikipedia.database.DatabaseClient;
import org.wikipedia.model.EnumCode;

import java.util.Collection;

public abstract class AsyncDao<Status extends EnumCode, Dat, Row extends AsyncRow<Status, Dat>>
        extends BaseDao<Row> {
    protected AsyncDao(@NonNull DatabaseClient<Row> client) {
        super(client);
    }

    public void startTransaction(@NonNull Collection<Row> rows) {
        for (Row row : rows) {
            startTransaction(row);
        }
    }

    public void completeTransaction(@NonNull Row row) {
        long timestamp = System.currentTimeMillis();
        completeTransaction(row, timestamp);
    }

    /** @return true if completable. */
    public synchronized boolean completeTransaction(@NonNull Row row, long timestamp) {
        if (completableTransaction(row)) {
            row.completeTransaction(timestamp);
            return true;
        }
        return false;
    }

    public void failTransaction(@NonNull Collection<Row> rows) {
        for (Row row : rows) {
            failTransaction(row);
        }
    }

    public synchronized void failTransaction(@NonNull Row row) {
        if (completableTransaction(row)) {
            row.failTransaction();
            upsert(row);
        }
    }

    protected void resetTransaction(@NonNull Row row, @NonNull Status status) {
        row.resetTransaction(status);
        upsert(row);
    }

    protected void startTransaction(@NonNull Row row) {
        row.startTransaction();
        upsert(row);
    }

    private boolean completableTransaction(@NonNull Row row) {
        Row query = queryPrimaryKey(row);
        return row.completable(query);
    }
}
