package org.wikipedia.history;

import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.data.ContentPersister;

import android.util.Log;

/**
 * Save the history entry for the specified page.
 */
public class SaveHistoryTask extends SaneAsyncTask<Void> {
    private final HistoryEntry entry;
    private final WikipediaApp app;

    public SaveHistoryTask(HistoryEntry entry, WikipediaApp app) {
        this.entry = entry;
        this.app = app;
    }

    @Override
    public Void performTask() throws Throwable {
        // Instead of "upserting" the history entry, we'll delete and re-persist it.
        // This is because upserting will update all previous instances of the history entry,
        // and won't collapse them into a single entry at the top. Deleting it will ensure
        // that all previous instances will be deleted, and then only the most recent instance
        // will be placed at the top.
        final ContentPersister persister = app.getPersister(HistoryEntry.class);
        persister.delete(entry, HistoryEntry.PERSISTENCE_HELPER.SELECTION_KEYS);
        persister.persist(entry);
        return null;
    }

    @Override
    public void onCatch(Throwable caught) {
        Log.w("SaveHistoryTask", "Caught " + caught.getMessage(), caught);
    }
}
