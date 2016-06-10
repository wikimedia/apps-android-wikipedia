package org.wikipedia.feed.continuereading;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.database.contract.PageHistoryContract;
import org.wikipedia.database.contract.PageImageHistoryContract;
import org.wikipedia.history.HistoryEntry;

import java.util.Date;

public class LastPageReadTask extends SaneAsyncTask<HistoryEntry> {
    @NonNull private final Context context;
    private final int age;
    private final long earlierThanTime;

    public LastPageReadTask(@NonNull Context context, int age, int minDaysOld) {
        this.context = context;
        this.age = age;
        earlierThanTime = new Date().getTime() - (minDaysOld * DateUtils.DAY_IN_MILLIS);
    }

    @Nullable @Override public HistoryEntry performTask() throws Throwable {
        Cursor cursor = queryLastPage(earlierThanTime);
        if (cursor == null) {
            return null;
        }

        try {
            if (cursor.moveToPosition(age)) {
                HistoryEntry entry = HistoryEntry.DATABASE_TABLE.fromCursor(cursor);
                entry.getTitle().setThumbUrl(PageImageHistoryContract.Col.IMAGE_NAME.val(cursor));
                return entry;
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    @Nullable private Cursor queryLastPage(long earlierThanTime) {
        ContentProviderClient client = HistoryEntry.DATABASE_TABLE.acquireClient(context);
        try {
            Uri uri = PageHistoryContract.PageWithImage.URI;
            final String[] projection = null;
            final String selection = PageHistoryContract.Col.TIMESTAMP.getName() + " < ?";
            final String[] selectionArgs = {Long.toString(earlierThanTime)};
            String order = PageHistoryContract.PageWithImage.ORDER_MRU + " limit " + (age + 1);
            return client.query(uri, projection, selection, selectionArgs, order);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            client.release();
        }
    }
}