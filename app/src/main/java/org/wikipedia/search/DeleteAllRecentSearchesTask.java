package org.wikipedia.search;

import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;
import android.content.Context;

/** AsyncTask to clear out recent search entries. */
public class DeleteAllRecentSearchesTask extends SaneAsyncTask<Void> {
    private final WikipediaApp app;

    public DeleteAllRecentSearchesTask(Context context) {
        super(SINGLE_THREAD);
        app = (WikipediaApp) context.getApplicationContext();
    }

    @Override
    public Void performTask() throws Throwable {
        app.getPersister(RecentSearch.class).deleteAll();
        return null;
    }
}
