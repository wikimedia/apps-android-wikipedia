package org.wikipedia.database.http;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.DatabaseClient;
import org.wikipedia.database.async.DefaultAsyncRow;
import org.wikipedia.database.async.AsyncRow;
import org.wikipedia.useroption.database.UserOptionDatabaseTable;

import java.util.ArrayList;
import java.util.Collection;

public abstract class HttpRowDao<T extends AsyncRow<HttpStatus>> {
    @NonNull private final DatabaseClient<T> client;
    /**
     * @param client Database client singleton. No writes should be performed to the table outside
     *               of SyncRowDao.
     */
    public HttpRowDao(@NonNull DatabaseClient<T> client) {
        this.client = client;
    }

    protected synchronized void upsert(@NonNull T item) {
        T local = queryItem(item);
        switch (local == null ? HttpStatus.ADDED : local.status()) {
            case SYNCHRONIZED:
            case OUTDATED:
            case MODIFIED:
                modifyTransaction(item);
                break;
            case ADDED:
            case DELETED:
                addTransaction(item);
                break;
            default:
                throw new RuntimeException("status=" + item.status());
        }
    }

    protected synchronized void update(@NonNull T item) {
        T local = queryItem(item);
        switch (local == null ? HttpStatus.SYNCHRONIZED : local.status()) {
            case SYNCHRONIZED:
            case MODIFIED:
            case ADDED:
            case DELETED:
                insertTransaction(item, HttpStatus.OUTDATED);
                break;
            case OUTDATED:
                break;
            default:
                throw new RuntimeException("status=" + item.status());
        }
    }

    protected synchronized void delete(@NonNull T item) {
        T local = queryItem(item);
        switch (local == null ? HttpStatus.DELETED : local.status()) {
            case SYNCHRONIZED:
            case OUTDATED:
            case MODIFIED:
            case ADDED:
                delete(item);
                break;
            case DELETED:
                break;
            default:
                throw new RuntimeException("status=" + item.status());
        }
    }

    /**
     * Delete all table rows but don't update service state. For example, a user logs out and all
     * private data stored locally should be removed. If the sync adapter account is not removed,
     * the data may be repopulated.
     */
    public synchronized void clear() {
        client.deleteAll();
    }

    public synchronized void reconcile(@NonNull T item) {
        completeTransaction(item, System.currentTimeMillis());

        // TODO: delete items no longer present in the database. The passed in list of items is
        //       expected to be the full list of items available on the service. After upserting,
        //       delete anything older than the current timestamp.
    }

    @NonNull public synchronized Collection<T> startTransaction() {
        Collection<T> items = querySyncable();
        for (T item : items) {
            item.startTransaction();
            insertItem(item);
        }
        return items;
    }

    public synchronized void resetTransaction(@NonNull T item) {
        if (!completable(item)) {
            return;
        }

        item.resetTransaction(item.status());
        insertItem(item);
    }

    public void completeTransaction(@NonNull T item) {
        long timestamp = System.currentTimeMillis();
        completeTransaction(item, timestamp);
    }

    public synchronized void completeTransaction(@NonNull T item, long timestamp) {
        if (!completable(item)) {
            return;
        }

        switch (item.status()) {
            case SYNCHRONIZED:
            case OUTDATED:
            case MODIFIED:
            case ADDED:
                item.completeTransaction(timestamp);
                insertItem(item);
                break;
            case DELETED:
                removeItem(item);
                break;
            default:
                throw new RuntimeException("status=" + item.status());
        }
    }

    private boolean completable(@NonNull T item) {
        T local = queryItem(item);
        return item.completeable(local);
    }

    @NonNull private Collection<T> querySyncable() {
        String[] selectionArgs = null;
        String selection = UserOptionDatabaseTable.Col.HTTP.status() + " != " + HttpStatus.SYNCHRONIZED.code() + " and "
                + UserOptionDatabaseTable.Col.HTTP.transactionId() + " == " + DefaultAsyncRow.NO_TRANSACTION_ID;
        String sortOrder = null;
        Cursor cursor = client.select(selection, selectionArgs, sortOrder);
        return cursorToCollection(cursor);
    }

    @NonNull private Collection<T> cursorToCollection(@NonNull Cursor cursor) {
        Collection<T> ret = new ArrayList<>();
        while (cursor.moveToNext()) {
            ret.add(client.fromCursor(cursor));
        }
        return ret;
    }

    private void addTransaction(@NonNull T item) {
        insertTransaction(item, HttpStatus.ADDED);
    }

    private void modifyTransaction(@NonNull T item) {
        insertTransaction(item, HttpStatus.MODIFIED);
    }

    private void insertTransaction(@NonNull T item, @NonNull HttpStatus status) {
        item.resetTransaction(status);
        insertItem(item);
    }

    @Nullable protected T queryItem(@NonNull T item) {
        String[] selectionArgs = client.getPrimaryKeySelectionArgs(item);
        String selection = client.getPrimaryKeySelection(item, selectionArgs);
        String sortOrder = null;
        Cursor cursor = client.select(selection, selectionArgs, sortOrder);
        return cursor.moveToNext() ? client.fromCursor(cursor) : null;
    }

    private synchronized void removeItem(@NonNull T item) {
        String[] selectionArgs = client.getPrimaryKeySelectionArgs(item);
        client.delete(item, selectionArgs);
    }

    protected synchronized void insertItem(@NonNull T item) {
        client.persist(item);
    }
}
