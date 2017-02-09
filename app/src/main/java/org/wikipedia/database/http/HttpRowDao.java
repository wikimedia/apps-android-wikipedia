package org.wikipedia.database.http;

import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseClient;
import org.wikipedia.database.async.AsyncDao;

public class HttpRowDao<Dat, Row extends HttpRow<Dat>> extends AsyncDao<HttpStatus, Dat, Row> {
    /**
     * @param client Database client singleton. No writes should be performed to the table outside
     *               of SyncRowDao.
     */
    public HttpRowDao(@NonNull DatabaseClient<Row> client) {
        super(client);
    }

    // TODO: most clients just have a Dat. Should the input be that instead?
    public synchronized void markUpserted(@NonNull Row row) {
        Row query = queryPrimaryKey(row);
        switch (query == null ? HttpStatus.DELETED : query.status()) {
            case SYNCHRONIZED:
            case OUTDATED:
            case MODIFIED:
                resetTransaction(row, HttpStatus.MODIFIED);
                break;
            case DELETED:
                resetTransaction(row, HttpStatus.ADDED);
                break;
            case ADDED:
                break;
            default:
                throw new RuntimeException("status=" + row.status());
        }
    }

    public synchronized void markOutdated(@NonNull Row row) {
        Row query = queryPrimaryKey(row);
        switch (query == null ? HttpStatus.SYNCHRONIZED : query.status()) {
            case SYNCHRONIZED:
            case MODIFIED:
            case ADDED:
            case DELETED:
                resetTransaction(row, HttpStatus.OUTDATED);
                break;
            case OUTDATED:
                break;
            default:
                throw new RuntimeException("status=" + row.status());
        }
    }

    public synchronized void markDeleted(@NonNull Row row) {
        Row query = queryPrimaryKey(row);
        switch (query == null ? HttpStatus.DELETED : query.status()) {
            case SYNCHRONIZED:
            case OUTDATED:
            case MODIFIED:
            case ADDED:
                resetTransaction(row, HttpStatus.DELETED);
                break;
            case DELETED:
                break;
            default:
                throw new RuntimeException("status=" + row.status());
        }
    }

    @Override
    public synchronized boolean completeTransaction(@NonNull Row row, long timestamp) {
        if (super.completeTransaction(row, timestamp)) {
            if (row.status() == HttpStatus.DELETED) {
                delete(row);
            } else {
                upsert(row);
            }
            return true;
        }
        return false;
    }
}
