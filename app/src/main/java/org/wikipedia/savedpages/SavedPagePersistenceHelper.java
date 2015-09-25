package org.wikipedia.savedpages;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.data.PersistenceHelper;

import java.util.Date;

public class SavedPagePersistenceHelper extends PersistenceHelper<SavedPage> {

    private static final int DB_VER_INTRODUCED = 4;
    private static final int DB_VER_NAMESPACE_ADDED = 6;

    private static final String COL_SITE = "site";
    private static final String COL_TITLE = "title";
    private static final String COL_NAMESPACE = "namespace";
    private static final String COL_TIMESTAMP = "timestamp";

    public static final String[] SELECTION_KEYS = {
            COL_SITE,
            COL_NAMESPACE,
            COL_TITLE
    };

    @Override
    public SavedPage fromCursor(Cursor c) {
        Site site = new Site(c.getString(c.getColumnIndex(COL_SITE)));
        PageTitle title = new PageTitle(c.getString(c.getColumnIndex(COL_NAMESPACE)),
                c.getString(c.getColumnIndex(COL_TITLE)), site);
        Date timestamp = new Date(c.getLong(c.getColumnIndex(COL_TIMESTAMP)));
        return new SavedPage(title, timestamp);
    }

    @Override
    protected ContentValues toContentValues(SavedPage obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_SITE, obj.getTitle().getSite().getDomain());
        contentValues.put(COL_TITLE, obj.getTitle().getText());
        contentValues.put(COL_NAMESPACE, obj.getTitle().getNamespace());
        contentValues.put(COL_TIMESTAMP, obj.getTimestamp().getTime());
        return contentValues;
    }

    public boolean savedPageExists(WikipediaApp app, PageTitle title) {
        Cursor c = null;
        boolean exists = false;
        try {
            SavedPage savedPage = new SavedPage(title);
            String[] args = getPrimaryKeySelectionArgs(savedPage);
            String selection = getPrimaryKeySelection(savedPage, args);
            c = app.getPersister(SavedPage.class).select(selection, args, "");
            if (c.getCount() > 0) {
                exists = true;
            }
        } catch (SQLiteException e) {
            // page title doesn't exist in database... no problem if it fails.
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return exists;
    }

    @Override
    public String getTableName() {
        return "savedpages";
    }

    @Override
    protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
    }

    @Override
    public Column[] getColumnsAdded(int version) {
        switch (version) {
            case 1:
                return new Column[] {
                        new Column("_id", "integer primary key"),
                        new Column(COL_SITE, "string"),
                        new Column(COL_TITLE, "string"),
                        new Column(COL_TIMESTAMP, "integer")
                };
            case DB_VER_NAMESPACE_ADDED:
                return new Column[] {
                        new Column(COL_NAMESPACE, "string")
                };
            default:
                return new Column[0];
        }
    }

    @Override
    public String getPrimaryKeySelection(@NonNull SavedPage obj, @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(obj, SELECTION_KEYS);
    }

    @Override
    protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull SavedPage obj) {
        return new String[] {
                obj.getTitle().getSite().getDomain(),
                obj.getTitle().getNamespace(),
                obj.getTitle().getText()
        };
    }
}
