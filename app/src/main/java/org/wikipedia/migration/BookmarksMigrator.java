package org.wikipedia.migration;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.savedpages.SavedPage;

import java.util.Date;

/**
 * The Beta application had a 'bookmarks' table for a week, which was
 * renamed savedpages when saved pages as a feature made an appearance
 * again. This made people lose their old bookmarks.
 *
 * We check if there exists a bookmarks table, and if so, if there are
 * any entries there that aren't in the current saved pages table. If
 * such entries exist, we just reinsert them.
 *
 * This code can be removed safely in a couple of weeks after the beta,
 * since this will not be needed after that. One time migration, isn't it?
 */
public class BookmarksMigrator {
    private static final int COL_INDEX_SITE = 1;
    private static final int COL_INDEX_TITLE = 2;
    private static final int COL_INDEX_TIME = 3;
    private final WikipediaApp app;

    public BookmarksMigrator(WikipediaApp app) {
        this.app = app;
    }

    /**
     * Check if migration is necessary, and if so, perform it
     *
     * @return true if migration was performed, false otherwise
     */
    public boolean migrateIfNeeded() {
        SQLiteDatabase db = app.getDbOpenHelper().getWritableDatabase();
        Cursor cur;
        try {
            cur = db.rawQuery("SELECT * FROM bookmarks", null);
        } catch (SQLiteException ex) {
            // the table does not exist. this is a fresh install
            // Nothing to migrate. move along...
            return false;
        }
        int rowsFound = 0;
        cur.moveToFirst();
        while (!cur.isAfterLast()) {
            Site site = new Site(cur.getString(COL_INDEX_SITE));
            PageTitle title = new PageTitle(cur.getString(COL_INDEX_TITLE), site);
            Date timestamp = new Date(cur.getLong(COL_INDEX_TIME));
            SavedPage page = new SavedPage(title, timestamp);
            app.getPersister(SavedPage.class).upsert(page, SavedPage.PERSISTENCE_HELPER.SELECTION_KEYS);
            Log.d("Wikipedia", "Migrated " + title.getDisplayText());
            rowsFound += 1;
            cur.moveToNext();
        }
        cur.close();
        // Delete the table!
        db.execSQL("DROP TABLE IF EXISTS bookmarks");
        return rowsFound != 0;
    }
}
