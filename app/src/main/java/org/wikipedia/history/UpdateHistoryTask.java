package org.wikipedia.history;

import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.database.DatabaseClient;
import org.wikipedia.database.contract.PageHistoryContract;

import io.reactivex.functions.Action;

/**
 * Save the history entry for the specified page.
 */
public class UpdateHistoryTask implements Action {
    private final HistoryEntry entry;

    public UpdateHistoryTask(HistoryEntry entry) {
        this.entry = entry;
    }

    @Override
    public void run() throws Exception {
        DatabaseClient<HistoryEntry> client = WikipediaApp.getInstance().getDatabaseClient(HistoryEntry.class);
        client.upsert(new HistoryEntry(entry.getTitle(),
                        entry.getTimestamp(),
                        entry.getSource(),
                        entry.getTimeSpentSec() + getPreviousTimeSpent(client)),
                PageHistoryContract.Page.SELECTION);
    }

    private int getPreviousTimeSpent(@NonNull DatabaseClient<HistoryEntry> client) {
        int timeSpent = 0;
        String selection = ":siteCol == ? and :langCol == ? and :titleCol == ?"
                .replaceAll(":siteCol", PageHistoryContract.Page.SITE.qualifiedName())
                .replaceAll(":langCol", PageHistoryContract.Page.LANG.qualifiedName())
                .replaceAll(":titleCol", PageHistoryContract.Page.TITLE.qualifiedName());
        String[] selectionArgs = new String[]{entry.getTitle().getWikiSite().authority(),
                entry.getTitle().getWikiSite().languageCode(),
                entry.getTitle().getText()};
        Cursor cursor = client.select(selection, selectionArgs, null);
        if (cursor.moveToFirst()) {
            timeSpent = PageHistoryContract.Col.TIME_SPENT.val(cursor);
        }
        cursor.close();
        return timeSpent;
    }


}
