package org.wikipedia.search;

import android.content.Context;

import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;

/** AsyncTask to clear out recent search entries. */
public class DeleteAllRecentSearchesTask extends SaneAsyncTask<Void> {
    private final WikipediaApp app;

    public DeleteAllRecentSearchesTask(Context context) {
        app = (WikipediaApp) context.getApplicationContext();
    }

    @Override
    public Void performTask() throws Throwable {
        app.getDatabaseClient(RecentSearch.class).deleteAll();
        return null;
    }
}
