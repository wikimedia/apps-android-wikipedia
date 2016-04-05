package org.wikipedia.database.http;

import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseClient;
import org.wikipedia.database.async.AsyncDao;

import java.util.Collection;

public class HttpRowDao<T extends HttpRow> extends AsyncDao<HttpStatus, T> {
    /**
     * @param client Database client singleton. No writes should be performed to the table outside
     *               of SyncRowDao.
     */
    public HttpRowDao(@NonNull DatabaseClient<T> client) {
        super(client);
    }

    public synchronized void upsertTransaction(@NonNull T item) {
        T query = queryPrimaryKey(item);
        switch (query == null ? HttpStatus.DELETED : query.status()) {
            case SYNCHRONIZED:
            case OUTDATED:
            case MODIFIED:
                resetTransaction(item, HttpStatus.MODIFIED);
                break;
            case DELETED:
                resetTransaction(item, HttpStatus.ADDED);
                break;
            case ADDED:
                break;
            default:
                throw new RuntimeException("status=" + item.status());
        }
    }

    public synchronized void updateTransaction(@NonNull T item) {
        T query = queryPrimaryKey(item);
        switch (query == null ? HttpStatus.SYNCHRONIZED : query.status()) {
            case SYNCHRONIZED:
            case MODIFIED:
            case ADDED:
            case DELETED:
                resetTransaction(item, HttpStatus.OUTDATED);
                break;
            case OUTDATED:
                break;
            default:
                throw new RuntimeException("status=" + item.status());
        }
    }

    public synchronized void deleteTransaction(@NonNull T item) {
        T query = queryPrimaryKey(item);
        switch (query == null ? HttpStatus.DELETED : query.status()) {
            case SYNCHRONIZED:
            case OUTDATED:
            case MODIFIED:
            case ADDED:
                resetTransaction(item, HttpStatus.DELETED);
                break;
            case DELETED:
                break;
            default:
                throw new RuntimeException("status=" + item.status());
        }
    }

    public void reconcileTransaction(@NonNull Collection<T> items) {
        for (T item : items) {
            completeTransaction(item);
        }

        // TODO: delete items no longer present in the database. The passed in list of items is
        //       expected to be the full list of items available on the service. After upserting,
        //       delete anything older than the current timestamp.
    }

    public void completeTransaction(@NonNull T item) {
        long timestamp = System.currentTimeMillis();
        completeTransaction(item, timestamp);
    }

    @Override
    public synchronized boolean completeTransaction(@NonNull T item, long timestamp) {
        if (super.completeTransaction(item, timestamp)) {
            if (item.status() == HttpStatus.DELETED) {
                delete(item);
            }
            return true;
        }
        return false;
    }
}