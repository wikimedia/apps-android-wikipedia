package org.wikipedia.history;

import android.content.Context;

import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;

/** AsyncTask to clear out article history entries. */
public class DeleteAllHistoryTask extends SaneAsyncTask<Void> {
    private final WikipediaApp app;

    public DeleteAllHistoryTask(Context context) {
        app = (WikipediaApp) context.getApplicationContext();
    }

    @Override
    public Void performTask() throws Throwable {
        app.getDatabaseClient(HistoryEntry.class).deleteAll();
        return null;
    }
}
