package org.wikipedia.database;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;

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
        Uri uri = databaseTable.getBaseContentURI();
        try {
            client.insert(uri, databaseTable.toContentValues(obj));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public Cursor select(String selection, String[] selectionArgs, String sortOrder) {
        Uri uri = databaseTable.getBaseContentURI();
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
        Uri uri = databaseTable.getBaseContentURI();
        try {
            client.delete(uri, selection, selectionArgs);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(T obj, String[] selectionArgs) {
        Uri uri = databaseTable.getBaseContentURI();
        try {
            client.delete(
                    uri,
                    databaseTable.getPrimaryKeySelection(obj, selectionArgs),
                    databaseTable.getPrimaryKeySelectionArgs(obj)
            );
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void upsert(T obj, String[] selectionArgs) {
        Uri uri = databaseTable.getBaseContentURI();
        try {
            int rowsUpdated = client.update(
                    uri,
                    databaseTable.toContentValues(obj),
                    databaseTable.getPrimaryKeySelection(obj, selectionArgs),
                    databaseTable.getPrimaryKeySelectionArgs(obj)
            );
            if (rowsUpdated == 0) {
                // Insert!
                persist(obj);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}