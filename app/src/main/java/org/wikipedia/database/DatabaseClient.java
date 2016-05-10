package org.wikipedia.database;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class DatabaseClient<T> {
    @NonNull private final ContentProviderClient client;
    @NonNull private final DatabaseTable<T> databaseTable;

    public DatabaseClient(@NonNull Context context,
                          @NonNull DatabaseTable<T> databaseTable) {
        this(databaseTable.acquireClient(context), databaseTable);
    }

    public DatabaseClient(@NonNull ContentProviderClient client,
                          @NonNull DatabaseTable<T> databaseTable) {
        this.client = client;
        this.databaseTable = databaseTable;
    }

    public void persist(T obj) {
        try {
            client.insert(uri(), toContentValues(obj));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public Cursor select(@Nullable String selection, @Nullable String[] selectionArgs,
                         @Nullable String sortOrder) {
        return select(uri(), selection, selectionArgs, sortOrder);
    }

    public Cursor select(@NonNull Uri uri, @Nullable String selection,
                         @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        try {
            return client.query(uri, null, selection, selectionArgs, sortOrder);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteAll() {
        deleteWhere("", new String[] {});
    }

    public void deleteWhere(String selection, String[] selectionArgs) {
        try {
            client.delete(uri(), selection, selectionArgs);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(@NonNull T obj, @NonNull String[] selectionArgs) {
        try {
            client.delete(
                    uri(),
                    getPrimaryKeySelection(obj, selectionArgs),
                    getPrimaryKeySelectionArgs(obj)
            );
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: migrate old tables to use unique constraints and just call insertWithOnConflict.
    public void upsert(@NonNull T obj, @NonNull String[] selectionArgs) {
        try {
            int rowsUpdated = client.update(
                    uri(),
                    toContentValues(obj),
                    getPrimaryKeySelection(obj, selectionArgs),
                    getPrimaryKeySelectionArgs(obj)
            );
            if (rowsUpdated == 0) {
                // TODO: synchronize with other writes. There are two operations performed.
                persist(obj);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public T fromCursor(Cursor cursor) {
        return databaseTable.fromCursor(cursor);
    }

    public ContentValues toContentValues(T obj) {
        return databaseTable.toContentValues(obj);
    }

    public String getPrimaryKeySelection(@NonNull T obj, @NonNull String[] selectionArgs) {
        return databaseTable.getPrimaryKeySelection(obj, selectionArgs);
    }

    public String[] getPrimaryKeySelectionArgs(@NonNull T obj) {
        return databaseTable.getPrimaryKeySelectionArgs(obj);
    }

    protected Uri uri() {
        return databaseTable.getBaseContentURI();
    }
}
