package org.wikipedia.page.bottomcontent;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.database.contract.PageHistoryContract;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;

/**
 * Get a Read More topic for the main page. This is looking at the history table.
 * We're looking at the last history entry that is not of source main page or random.
 */
public class MainPageReadMoreTopicTask extends SaneAsyncTask<PageTitle> {
    private final Context context;

    public MainPageReadMoreTopicTask(Context context) {
        this.context = context;
    }

    @Override
    public PageTitle performTask() throws Throwable {
        Cursor c = getInterestedHistoryEntry();
        try {
            if (c.moveToFirst()) {
                final HistoryEntry historyEntry = HistoryEntry.DATABASE_TABLE.fromCursor(c);
                return historyEntry.getTitle();
            }
            return null;
        } finally {
            c.close();
        }
    }

    private Cursor getInterestedHistoryEntry() {
        ContentProviderClient client = HistoryEntry.DATABASE_TABLE.acquireClient(context);
        try {
            Uri uri = PageHistoryContract.Page.URI;
            final String[] projection = null;
            String selection = ":sourceCol != ? and :sourceCol != ? "
                    .replaceAll(":sourceCol", PageHistoryContract.Page.SOURCE.qualifiedName());
            String[] selectionArgs = new String[] {Integer.toString(HistoryEntry.SOURCE_MAIN_PAGE),
                    Integer.toString(HistoryEntry.SOURCE_RANDOM)};
            String order = PageHistoryContract.Page.ORDER_MRU;
            return client.query(uri, projection, selection, selectionArgs, order);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            client.release();
        }
    }
}
