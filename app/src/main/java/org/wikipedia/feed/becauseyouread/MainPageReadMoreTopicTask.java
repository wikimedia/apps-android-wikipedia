package org.wikipedia.feed.becauseyouread;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.database.contract.PageHistoryContract;
import org.wikipedia.database.contract.PageImageHistoryContract;
import org.wikipedia.history.HistoryEntry;

import java.util.concurrent.Callable;

/**
 * Get a Read More topic for the main page. This is looking at the history table.
 * We're looking at the last history entry that is not of source main page or random.
 */
public class MainPageReadMoreTopicTask implements Callable<HistoryEntry> {
    private int age;

    public MainPageReadMoreTopicTask(int age) {
        this.age = age;
    }

    @Override
    public HistoryEntry call() {
        try (Cursor c = getInterestedHistoryEntry()) {
            if (c.moveToPosition(age)) {
                HistoryEntry entry = HistoryEntry.DATABASE_TABLE.fromCursor(c);
                entry.getTitle().setThumbUrl(PageImageHistoryContract.Col.IMAGE_NAME.value(c));
                return entry.getTitle().isMainPage() ? null : entry;
            }
            return null;
        }
    }

    private Cursor getInterestedHistoryEntry() {
        Context context = WikipediaApp.getInstance();
        try (ContentProviderClient client = HistoryEntry.DATABASE_TABLE.acquireClient(context)) {
            Uri uri = PageHistoryContract.PageWithImage.URI;
            String selection = ":sourceCol != ? and :sourceCol != ? and :sourceCol != ? and :timeSpentCol >= ?"
                    .replaceAll(":sourceCol", PageHistoryContract.Page.SOURCE.qualifiedName())
                    .replaceAll(":timeSpentCol", PageHistoryContract.Page.TIME_SPENT.qualifiedName());
            String[] selectionArgs = new String[]{Integer.toString(HistoryEntry.SOURCE_MAIN_PAGE),
                    Integer.toString(HistoryEntry.SOURCE_RANDOM),
                    Integer.toString(HistoryEntry.SOURCE_FEED_MAIN_PAGE),
                    Integer.toString(context.getResources().getInteger(R.integer.article_engagement_threshold_sec))};
            String order = PageHistoryContract.PageWithImage.ORDER_MRU;
            return client.query(uri, null, selection, selectionArgs, order);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
