package org.wikipedia.feed.continuereading;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.database.contract.PageHistoryContract;
import org.wikipedia.history.HistoryEntry;

public class LastPageReadTask extends SaneAsyncTask<HistoryEntry> {
    public interface Callback {
        void success(@NonNull HistoryEntry entry);
    }

    @NonNull private final Context context;
    @NonNull private final Callback cb;

    public LastPageReadTask(@NonNull Context context, @NonNull Callback cb) {
        this.context = context;
        this.cb = cb;
    }

    @Nullable @Override public HistoryEntry performTask() throws Throwable {
        Cursor cursor = queryLastPage();
        if (cursor == null) {
            return null;
        }

        try {
            if (cursor.moveToNext()) {
                return HistoryEntry.DATABASE_TABLE.fromCursor(cursor);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    @Override public void onFinish(@Nullable HistoryEntry entry) {
        super.onFinish(entry);
        if (entry != null) {
            cb.success(entry);
        }
    }

    @Nullable private Cursor queryLastPage() {
        ContentProviderClient client = HistoryEntry.DATABASE_TABLE.acquireClient(context);
        try {
            Uri uri = PageHistoryContract.Page.URI;
            final String[] projection = null;
            final String selection = null;
            final String[] selectionArgs = null;
            String order = PageHistoryContract.Page.ORDER_MRU + " limit 1";
            return client.query(uri, projection, selection, selectionArgs, order);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            client.release();
        }
    }
}