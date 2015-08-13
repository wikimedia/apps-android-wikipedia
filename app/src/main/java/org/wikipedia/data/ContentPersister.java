package org.wikipedia.data;

import android.content.ContentProviderClient;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

public abstract class ContentPersister<T> {
    private final ContentProviderClient client;
    private final PersistenceHelper<T> persistenceHelper;

    public ContentPersister(ContentProviderClient client, PersistenceHelper<T> persistenceHelper) {
        this.client = client;
        this.persistenceHelper = persistenceHelper;
    }

    public void persist(T obj) {
        Uri uri = persistenceHelper.getBaseContentURI();
        try {
            client.insert(uri, persistenceHelper.toContentValues(obj));
        } catch (RemoteException e) {
            // This shouldn't happen
            throw new RuntimeException(e);
        }
    }

    public Cursor select(String selection, String[] selectionArgs, String sortOrder) {
        Uri uri = persistenceHelper.getBaseContentURI();
        Cursor c;
        try {
            c = client.query(uri, null, selection, selectionArgs, sortOrder);
        } catch (RemoteException e) {
            // This shouldn't happen
            throw new RuntimeException(e);
        }
        return c;
    }

    public void deleteAll() {
        deleteWhere("", new String[] {});
    }

    public void deleteWhere(String selection, String[] selectionArgs) {
        Uri uri = persistenceHelper.getBaseContentURI();
        try {
            client.delete(uri, selection, selectionArgs);
        } catch (RemoteException e) {
            // This also shouldn't happen
            throw new RuntimeException(e);
        }
    }

    public void delete(T obj, String[] selectionArgs) {
        Uri uri = persistenceHelper.getBaseContentURI();
        try {
            client.delete(
                    uri,
                    persistenceHelper.getPrimaryKeySelection(obj, selectionArgs),
                    persistenceHelper.getPrimaryKeySelectionArgs(obj)
            );
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void upsert(T obj, String[] selectionArgs) {
        Uri uri = persistenceHelper.getBaseContentURI();
        try {
            int rowsUpdated = client.update(
                    uri,
                    persistenceHelper.toContentValues(obj),
                    persistenceHelper.getPrimaryKeySelection(obj, selectionArgs),
                    persistenceHelper.getPrimaryKeySelectionArgs(obj)
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
