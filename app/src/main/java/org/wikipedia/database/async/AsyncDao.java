package org.wikipedia.database.async;

import android.support.annotation.NonNull;

import org.wikipedia.database.BaseDao;
import org.wikipedia.database.DatabaseClient;
import org.wikipedia.model.EnumCode;

import java.util.Collection;

public abstract class AsyncDao<Status extends EnumCode, Row extends AsyncRow<Status>> extends BaseDao<Row> {
    protected AsyncDao(@NonNull DatabaseClient<Row> client) {
        super(client);
    }

    public void startTransaction(@NonNull Collection<Row> rows) {
        for (Row row : rows) {
            startTransaction(row);
        }
    }

    /** @return true if completable. */
    public synchronized boolean completeTransaction(@NonNull Row row, long timestamp) {
        if (completableTransaction(row)) {
            row.completeTransaction(timestamp);
            upsert(row);
            return true;
        }
        return false;
    }

    public void failTransaction(@NonNull Collection<Row> rows) {
        for (Row row : rows) {
            failTransaction(row);
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

    protected synchronized void failTransaction(@NonNull Row row) {
        if (completableTransaction(row)) {
            row.failTransaction();
            upsert(row);
        }
    }

    private boolean completableTransaction(@NonNull Row row) {
        Row query = queryPrimaryKey(row);
        return row.completable(query);
    }

    private synchronized void upsert(@NonNull Row row) {
        insert(row);
    }
}