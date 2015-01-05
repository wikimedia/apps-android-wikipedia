package org.wikipedia.data;

import android.content.ContentProviderClient;
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

    public void delete(T obj) {
        Uri uri = persistenceHelper.getBaseContentURI();
        try {
            client.delete(
                    uri,
                    persistenceHelper.getPrimaryKeySelection(),
                    persistenceHelper.getPrimaryKeySelectionArgs(obj)
            );
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void upsert(T obj) {
        Uri uri = persistenceHelper.getBaseContentURI();
        try {
            int rowsUpdated = client.update(
                    uri,
                    persistenceHelper.toContentValues(obj),
                    persistenceHelper.getPrimaryKeySelection(),
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


    public void cleanup() {
        this.client.release();
    }
}
