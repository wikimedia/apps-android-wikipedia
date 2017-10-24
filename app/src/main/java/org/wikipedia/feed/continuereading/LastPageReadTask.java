package org.wikipedia.feed.continuereading;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

import org.wikipedia.R;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.database.contract.PageHistoryContract;
import org.wikipedia.database.contract.PageImageHistoryContract;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.util.ContentProviderClientCompat;

import java.util.Date;

public class LastPageReadTask extends SaneAsyncTask<HistoryEntry> {
    @NonNull private final Context context;
    private final int age;
    private final long earlierThanTime;
    private final long noEarlierThanTime;

    public LastPageReadTask(@NonNull Context context, int age, int minDaysOld, int maxDaysOld) {
        this.context = context;
        this.age = age;
        earlierThanTime = new Date().getTime() - (minDaysOld * DateUtils.DAY_IN_MILLIS);
        noEarlierThanTime  = new Date().getTime() - (maxDaysOld * DateUtils.DAY_IN_MILLIS);
    }

    @Nullable @Override public HistoryEntry performTask() throws Throwable {
        Cursor cursor = queryLastPage(earlierThanTime, noEarlierThanTime);
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

    @Nullable private Cursor queryLastPage(long earlierThanTime, long noEarlierThanTime) {
        ContentProviderClient client = HistoryEntry.DATABASE_TABLE.acquireClient(context);
        try {
            Uri uri = PageHistoryContract.PageWithImage.URI;
            final String[] projection = null;
            final String selection = ":timestampCol < ? and :timestampCol >= ? and :sourceCol != ? and :sourceCol != ? and :timeSpentCol >= ?"
                    .replaceAll(":timestampCol", PageHistoryContract.Col.TIMESTAMP.getName())
                    .replaceAll(":sourceCol", PageHistoryContract.Page.SOURCE.qualifiedName())
                    .replaceAll(":timeSpentCol", PageHistoryContract.Page.TIME_SPENT.qualifiedName());
            final String[] selectionArgs = {Long.toString(earlierThanTime),
                    Long.toString(noEarlierThanTime),
                    Integer.toString(HistoryEntry.SOURCE_MAIN_PAGE),
                    Integer.toString(HistoryEntry.SOURCE_FEED_MAIN_PAGE),
                    Integer.toString(context.getResources().getInteger(R.integer.article_engagement_threshold_sec))};
            String order = PageHistoryContract.PageWithImage.ORDER_MRU + " limit " + (age + 1);
            return client.query(uri, projection, selection, selectionArgs, order);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            ContentProviderClientCompat.close(client);
        }
    }
}
