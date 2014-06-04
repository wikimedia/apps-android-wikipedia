package org.wikipedia.data;

import android.content.ContentProviderClient;
import android.net.Uri;
import android.os.RemoteException;

public abstract class ContentPersister<T> {
    private final ContentProviderClient client;
    private final PersistanceHelper<T> persistanceHelper;

    public ContentPersister(ContentProviderClient client, PersistanceHelper<T> persistanceHelper) {
        this.client = client;
        this.persistanceHelper = persistanceHelper;
    }

    public void persist(T obj) {
        Uri uri = persistanceHelper.getBaseContentURI();
        try {
            client.insert(uri, persistanceHelper.toContentValues(obj));
        } catch (RemoteException e) {
            // This shouldn't happen
            throw new RuntimeException(e);
        }
    }

    public void deleteAll() {
        deleteWhere("", new String[] {});
    }

    public void deleteWhere(String selection, String[] selectionArgs) {
        Uri uri = persistanceHelper.getBaseContentURI();
        try {
            client.delete(uri, selection, selectionArgs);
        } catch (RemoteException e) {
            // This also shouldn't happen
            throw new RuntimeException(e);
        }
    }

    public void delete(T obj) {
        Uri uri = persistanceHelper.getBaseContentURI();
        try {
            client.delete(
                    uri,
                    persistanceHelper.getPrimaryKeySelection(),
                    persistanceHelper.getPrimaryKeySelectionArgs(obj)
            );
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void upsert(T obj) {
        Uri uri = persistanceHelper.getBaseContentURI();
        try {
            int rowsUpdated = client.update(
                    uri,
                    persistanceHelper.toContentValues(obj),
                    persistanceHelper.getPrimaryKeySelection(),
                    persistanceHelper.getPrimaryKeySelectionArgs(obj)
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
