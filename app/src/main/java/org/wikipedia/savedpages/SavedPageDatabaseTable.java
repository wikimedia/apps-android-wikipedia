package org.wikipedia.savedpages;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.database.DatabaseTable;
import org.wikipedia.util.StringUtil;

import java.io.File;
import java.util.Date;

public class SavedPageDatabaseTable extends DatabaseTable<SavedPage> {

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

    /** Requires database of version {@link #DB_VER_NAMESPACE_ADDED} or greater. */
    @Override
    public SavedPage fromCursor(Cursor cursor) {
        return fromPreNamespaceCursor(cursor,
                cursor.getString(cursor.getColumnIndex(COL_NAMESPACE)));
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
            c = app.getDatabaseClient(SavedPage.class).select(selection, args, "");
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
    protected void convertAllTitlesToUnderscores(SQLiteDatabase db) {
        Cursor cursor = db.query(getTableName(), null, null, null, null, null, null);
        int idIndex = cursor.getColumnIndex("_id");
        int titleIndex = cursor.getColumnIndex(COL_TITLE);
        ContentValues values = new ContentValues();
        while (cursor.moveToNext()) {
            String title = cursor.getString(titleIndex);
            if (title.contains(" ")) {
                values.put(COL_TITLE, title.replace(" ", "_"));
                String id = Long.toString(cursor.getLong(idIndex));
                db.updateWithOnConflict(getTableName(), values, "_id = ?", new String[]{id}, SQLiteDatabase.CONFLICT_REPLACE);

                SavedPage obj = hasNamespace(db) ? fromCursor(cursor) : fromPreNamespaceCursor(cursor);
                File newDir = new File(SavedPage.getSavedPagesDir() + "/" + obj.getTitle().getIdentifier());
                new File(SavedPage.getSavedPagesDir() + "/" + getSavedPageDir(obj, title)).renameTo(newDir);
            }
        }
        cursor.close();
    }

    private boolean hasNamespace(@NonNull SQLiteDatabase db) {
        return db.getVersion() >= DB_VER_NAMESPACE_ADDED;
    }

    private SavedPage fromPreNamespaceCursor(@NonNull Cursor cursor) {
        return fromPreNamespaceCursor(cursor, null);
    }

    private SavedPage fromPreNamespaceCursor(@NonNull Cursor cursor, @Nullable String namespace) {
        Site site = new Site(cursor.getString(cursor.getColumnIndex(COL_SITE)));
        PageTitle title = new PageTitle(namespace,
                cursor.getString(cursor.getColumnIndex(COL_TITLE)), site);
        Date timestamp = new Date(cursor.getLong(cursor.getColumnIndex(COL_TIMESTAMP)));
        return new SavedPage(title, timestamp);
    }

    private String getSavedPageDir(SavedPage page, String originalTitleText) {
        try {
            JSONObject json = new JSONObject();
            json.put("namespace", page.getTitle().getNamespace());
            json.put("text", originalTitleText);
            json.put("fragment", page.getTitle().getFragment());
            json.put("site", page.getTitle().getSite().getDomain());
            return StringUtil.md5string(json.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
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
