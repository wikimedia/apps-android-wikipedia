package org.wikipedia.beta.migration;

import android.util.Log;
import org.json.JSONObject;
import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.concurrency.SaneAsyncTask;

import java.util.List;

public class PerformMigrationsTask extends SaneAsyncTask<Boolean> {
    public PerformMigrationsTask() {
        super(SINGLE_THREAD);
    }

    @Override
    public Boolean performTask() throws Throwable {
        DataMigrator dataMigrator = new DataMigrator(WikipediaApp.getInstance());
        if (dataMigrator.hasData()) {
            // whee
            Log.d("Wikipedia", "Migrating old app data...");
            ArticleImporter articleImporter = new ArticleImporter(WikipediaApp.getInstance());
            List<JSONObject> pages = dataMigrator.extractSavedPages();
            Log.d("Wikipedia", "Importing " + pages.size() + " old saved pages as new saved pages...");
            articleImporter.importArticles(pages);
            Log.d("Wikipedia", "Deleting old saved pages table");
            dataMigrator.removeOldData();
            Log.d("Wikipedia", "Migration done.");
        } else {
            Log.d("Wikipedia", "No old app data to migrate");
        }

        BookmarksMigrator bookmarksMigrator = new BookmarksMigrator(WikipediaApp.getInstance());
        if (bookmarksMigrator.migrateIfNeeded()) {
            Log.d("Wikipedia", "Bookmarks migrator successfully run");
        } else {
            Log.d("Wikipedia", "No bookmarks needed migrating");
        }
        return true;
    }

    @Override
    public void onCatch(Throwable caught) {
        // Do nothing, really
        caught.printStackTrace();
    }
}
