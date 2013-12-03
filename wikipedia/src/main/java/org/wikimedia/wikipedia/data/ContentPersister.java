package org.wikimedia.wikipedia.data;

import android.content.ContentProviderClient;
import android.net.Uri;
import android.os.RemoteException;

abstract public class ContentPersister<T> {
    private final ContentProviderClient client;
    private final PersistanceHelper<T> persistanceHelper;

    public ContentPersister(ContentProviderClient client, PersistanceHelper<T> persistanceHelper) {
        this.client = client;
        this.persistanceHelper = persistanceHelper;
    }

    public void persist(T obj) {
        Uri uri = Uri.parse("content://" +
                        SQLiteContentProvider.getAuthorityForTable(persistanceHelper.getTableName()) +
                        "/" + persistanceHelper.getTableName());

        try {
            client.insert(uri, persistanceHelper.toContentValues(obj));
        } catch (RemoteException e) {
            // This shouldn't happen
            throw new RuntimeException(e);
        }
    }

    public void cleanup() {
        this.client.release();
    }
}
