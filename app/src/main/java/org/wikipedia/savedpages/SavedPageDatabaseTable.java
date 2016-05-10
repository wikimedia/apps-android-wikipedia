package org.wikipedia.savedpages;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Site;
import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.contract.SavedPageContract;
import org.wikipedia.database.contract.SavedPageContract.Col;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.FileUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;

import java.io.File;
import java.util.Date;

public class SavedPageDatabaseTable extends DatabaseTable<SavedPage> {
    private static final int DB_VER_INTRODUCED = 4;
    private static final int DB_VER_NAMESPACE_ADDED = 6;
    private static final int DB_VER_NORMALIZED_TITLES = 8;
    private static final int DB_VER_LANG_ADDED = 10;

    public SavedPageDatabaseTable() {
        super(SavedPageContract.TABLE, SavedPageContract.Page.URI);
    }

    /** Requires database of version {@link #DB_VER_LANG_ADDED} or greater. */
    @Override
    public SavedPage fromCursor(Cursor cursor) {
        return fromPreNamespaceCursor(cursor, Col.NAMESPACE.val(cursor), Col.LANG.val(cursor));
    }

    @Override
    protected ContentValues toContentValues(SavedPage obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Col.SITE.getName(), obj.getTitle().getSite().authority());
        contentValues.put(Col.LANG.getName(), obj.getTitle().getSite().languageCode());
        contentValues.put(Col.TITLE.getName(), obj.getTitle().getText());
        contentValues.put(Col.NAMESPACE.getName(), obj.getTitle().getNamespace());
        contentValues.put(Col.TIMESTAMP.getName(), obj.getTimestamp().getTime());
        return contentValues;
    }

    public Cursor queryAll(SQLiteDatabase db) {
        return db.query(getTableName(), null, null, null, null, null, null);
    }

    @Override
    protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
    }

    @Override
    protected void upgradeSchema(@NonNull SQLiteDatabase db, int toVersion) {
        switch (toVersion) {
            case DB_VER_NORMALIZED_TITLES:
                convertAllTitlesToUnderscores(db);
                break;
            case DB_VER_LANG_ADDED:
                addLangToAllSites(db);
                break;
            default:
                super.upgradeSchema(db, toVersion);
        }
    }

    /**
     * One-time fix for the inconsistencies in title formats all over the database. This migration will enforce
     * all titles stored in the database to follow the "Underscore_format" instead of the "Human readable form"
     * TODO: Delete this code after April 2016
     *
     * @param db Database object
     */
    private void convertAllTitlesToUnderscores(SQLiteDatabase db) {
        Cursor cursor = db.query(getTableName(), null, null, null, null, null, null);
        ContentValues values = new ContentValues();
        while (cursor.moveToNext()) {
            String title = Col.TITLE.val(cursor);
            if (title.contains(" ")) {
                values.put(Col.TITLE.getName(), title.replace(" ", "_"));
                String id = Long.toString(Col.ID.val(cursor));
                db.updateWithOnConflict(getTableName(), values, Col.ID.getName() + " = ?",
                        new String[]{id}, SQLiteDatabase.CONFLICT_REPLACE);

                SavedPage obj = fromPreNamespaceCursor(cursor, Col.NAMESPACE.val(cursor), null);
                String savedPageBaseDir = FileUtil.savedPageBaseDir();
                File newDir = new File(savedPageBaseDir + "/" + obj.getTitle().getIdentifier());
                new File(savedPageBaseDir + "/" + getSavedPageDir(obj, title)).renameTo(newDir);
            }
        }
        cursor.close();
    }

    private SavedPage fromPreNamespaceCursor(@NonNull Cursor cursor, @Nullable String namespace,
                                             @Nullable String lang) {
        String authority = Col.SITE.val(cursor);
        Site site = lang == null ? new Site(authority) : new Site(authority, lang);
        PageTitle title = new PageTitle(namespace, Col.TITLE.val(cursor), site);
        Date timestamp = Col.TIMESTAMP.val(cursor);
        return new SavedPage(title, timestamp);
    }

    private String getSavedPageDir(SavedPage page, String originalTitleText) {
        try {
            JSONObject json = new JSONObject();
            json.put("namespace", page.getTitle().getNamespace());
            json.put("text", originalTitleText);
            json.put("fragment", page.getTitle().getFragment());
            json.put("site", page.getTitle().getSite().authority());
            return StringUtil.md5string(json.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    @Override
    public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case DB_VER_INTRODUCED:
                return new Column<?>[] {Col.ID, Col.SITE, Col.TITLE, Col.TIMESTAMP};
            case DB_VER_NAMESPACE_ADDED:
                return new Column<?>[] {Col.NAMESPACE};
            case DB_VER_LANG_ADDED:
                return new Column<?>[] {Col.LANG};

            default:
                return super.getColumnsAdded(version);
        }
    }

    @Override
    public String getPrimaryKeySelection(@NonNull SavedPage obj, @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(obj, Col.SELECTION);
    }

    @Override
    protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull SavedPage obj) {
        return new String[] {
                obj.getTitle().getSite().authority(),
                obj.getTitle().getSite().languageCode(),
                obj.getTitle().getNamespace(),
                obj.getTitle().getText()
        };
    }

    // TODO: remove in September 2016.
    private void addLangToAllSites(@NonNull SQLiteDatabase db) {
        L.i("Adding language codes to " + getTableName());
        Cursor cursor = db.query(getTableName(), null, null, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                String site = Col.SITE.val(cursor);
                ContentValues values = new ContentValues();
                values.put(Col.LANG.getName(), site.split("\\.")[0]);
                String id = Long.toString(Col.ID.val(cursor));
                db.updateWithOnConflict(getTableName(), values, Col.ID.getName() + " = ?",
                        new String[]{id}, SQLiteDatabase.CONFLICT_REPLACE);
            }
        } finally {
            cursor.close();
        }
    }
}
