package org.wikipedia.database;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;

public abstract class BaseDao<T> {
    @NonNull private final DatabaseClient<T> client;

    protected BaseDao(@NonNull DatabaseClient<T> client) {
        this.client = client;
    }

    public synchronized void clear() {
        client.deleteAll();
    }

    protected synchronized void upsert(@NonNull T row) {
        // Implemented by AppContentProvider as an upsert.
        client.persist(row);
    }

    @Nullable protected T queryPrimaryKey(@NonNull T row) {
        String[] selectionArgs = client().getPrimaryKeySelectionArgs(row);
        String selection = client().getPrimaryKeySelection(row, selectionArgs);
        Collection<T> rows = query(selection, selectionArgs);
        return rows.isEmpty() ? null : rows.iterator().next();
    }

    @NonNull protected Collection<T> query(@Nullable String selection) {
        return query(selection, null);
    }

    @NonNull protected Collection<T> query(@Nullable String selection,
                                           @Nullable String[] selectionArgs) {
        return query(selection, selectionArgs, null);
    }

    @NonNull protected Collection<T> query(@Nullable String selection,
                                           @Nullable String[] selectionArgs,
                                           @Nullable String order) {
        Cursor cursor = client.select(selection, selectionArgs, order);
        try {
            return DbUtil.cursorToCollection(client, cursor);
        } finally {
            cursor.close();
        }
    }

    protected synchronized void delete(@NonNull T row) {
        String[] selectionArgs = client.getPrimaryKeySelectionArgs(row);
        client.delete(row, selectionArgs);
    }

    protected DatabaseClient<T> client() {
        return client;
    }
}
