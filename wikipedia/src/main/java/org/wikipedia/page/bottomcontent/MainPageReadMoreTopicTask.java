package org.wikipedia.page.bottomcontent;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.os.RemoteException;

import org.wikipedia.PageTitle;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.history.HistoryEntry;

/**
 * Get a Read More topic for the main page. This is looking at the history table.
 * We're looking at the last history entry that is not of source main page or random.
 */
public class MainPageReadMoreTopicTask extends SaneAsyncTask<PageTitle> {
    private final Context context;

    public MainPageReadMoreTopicTask(Context context) {
        super(SaneAsyncTask.SINGLE_THREAD);
        this.context = context;
    }

    @Override
    public PageTitle performTask() throws Throwable {
        Cursor c = getInterestedHistoryEntry();
        if (c.getCount() > 0) {
            c.moveToFirst();
            final HistoryEntry historyEntry = HistoryEntry.PERSISTENCE_HELPER.fromCursor(c);
            return historyEntry.getTitle();
        }
        return null;
    }

    private android.database.Cursor getInterestedHistoryEntry() {
        ContentProviderClient client = context.getContentResolver()
                .acquireContentProviderClient(HistoryEntry.PERSISTENCE_HELPER.getBaseContentURI());
        try {
            return client.query(
                    HistoryEntry.PERSISTENCE_HELPER.getBaseContentURI(),
                    null,
                    "source != ? AND source != ? ",
                    new String[] {Integer.toString(HistoryEntry.SOURCE_MAIN_PAGE),
                            Integer.toString(HistoryEntry.SOURCE_RANDOM)},
                    "timestamp DESC");
        } catch (RemoteException e) {
            // This shouldn't really be happening
            throw new RuntimeException(e);
        }
    }
}
