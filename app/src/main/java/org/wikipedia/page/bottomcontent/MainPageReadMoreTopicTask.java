package org.wikipedia.page.bottomcontent;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.database.contract.PageHistoryContract;
import org.wikipedia.database.contract.PageImageHistoryContract;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;

/**
 * Get a Read More topic for the main page. This is looking at the history table.
 * We're looking at the last history entry that is not of source main page or random.
 */
public class MainPageReadMoreTopicTask extends SaneAsyncTask<PageTitle> {
    private final Context context;
    private int age;

    public MainPageReadMoreTopicTask(Context context) {
        this(context, 0);
    }

    public MainPageReadMoreTopicTask(Context context, int age) {
        this.context = context;
        this.age = age;
    }

    @Override
    public PageTitle performTask() throws Throwable {
        Cursor c = getInterestedHistoryEntry();
        try {
            if (c.moveToPosition(age)) {
                HistoryEntry entry = HistoryEntry.DATABASE_TABLE.fromCursor(c);
                entry.getTitle().setThumbUrl(PageImageHistoryContract.Col.IMAGE_NAME.val(c));
                return entry.getTitle();
            }
            return null;
        } finally {
            c.close();
        }
    }

    private Cursor getInterestedHistoryEntry() {
        ContentProviderClient client = HistoryEntry.DATABASE_TABLE.acquireClient(context);
        try {
            Uri uri = PageHistoryContract.PageWithImage.URI;
            final String[] projection = null;
            String selection = ":sourceCol != ? and :sourceCol != ? "
                    .replaceAll(":sourceCol", PageHistoryContract.Page.SOURCE.qualifiedName());
            String[] selectionArgs = new String[] {Integer.toString(HistoryEntry.SOURCE_MAIN_PAGE),
                    Integer.toString(HistoryEntry.SOURCE_RANDOM)};
            String order = PageHistoryContract.PageWithImage.ORDER_MRU;
            return client.query(uri, projection, selection, selectionArgs, order);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            client.release();
        }
    }
}
