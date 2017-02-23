package org.wikipedia.savedpages;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.contract.SavedPageContract;
import org.wikipedia.database.contract.SavedPageContract.Col;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.FileUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;

import java.io.File;

/**
 * No longer used, but upgrade logic reserved for database version upgrades which occur sequentially
 * (see discussion at https://phabricator.wikimedia.org/rAPAWaf070d5914d3614b91be6b033961e39372241a92).
 */
public class SavedPageDatabaseTable extends DatabaseTable<SavedPage> {
    private static final int DB_VER_INTRODUCED = 4;
    private static final int DB_VER_NAMESPACE_ADDED = 6;
    private static final int DB_VER_NORMALIZED_TITLES = 8;
    private static final int DB_VER_LANG_ADDED = 10;
    private static final int DB_VER_DROPPED = 16;

    SavedPageDatabaseTable() {
        super(SavedPageContract.TABLE, SavedPageContract.Page.URI);
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
     * Converts all titles stored in the database to follow the underscore_format instead of the
     * normalized (or display) format.
     *
     * Preserved for database migrations from legacy versions.
     *
     * @param db Database object
     */
    private void convertAllTitlesToUnderscores(SQLiteDatabase db) {
        Cursor cursor = db.query(getTableName(), null, null, null, null, null, null);
        ContentValues values = new ContentValues();
        while (cursor.moveToNext()) {
            String titleStr = Col.TITLE.val(cursor);
            if (titleStr.contains(" ")) {
                values.put(Col.TITLE.getName(), titleStr.replace(" ", "_"));
                String id = Long.toString(Col.ID.val(cursor));
                db.updateWithOnConflict(getTableName(), values, Col.ID.getName() + " = ?",
                        new String[]{id}, SQLiteDatabase.CONFLICT_REPLACE);

                PageTitle pageTitle = fromPreNamespaceCursor(cursor, Col.NAMESPACE.val(cursor), null);
                String savedPageBaseDir = FileUtil.savedPageBaseDir();
                File newDir = new File(savedPageBaseDir + "/" + pageTitle.getIdentifier());
                new File(savedPageBaseDir + "/" + getSavedPageDir(pageTitle, titleStr)).renameTo(newDir);
            }
        }
        cursor.close();
    }

    /**
     * Preserved for database migrations from legacy versions.
     *
     * @param db Database object
     */
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

    private PageTitle fromPreNamespaceCursor(@NonNull Cursor cursor, @Nullable String namespace,
                                             @Nullable String lang) {
        String authority = Col.SITE.val(cursor);
        WikiSite wiki = lang == null ? new WikiSite(authority) : new WikiSite(authority, lang);
        return new PageTitle(namespace, Col.TITLE.val(cursor), wiki);
    }

    private String getSavedPageDir(PageTitle title, String originalTitleText) {
        try {
            JSONObject json = new JSONObject();
            json.put("namespace", title.getNamespace());
            json.put("text", originalTitleText);
            json.put("fragment", title.getFragment());
            json.put("site", title.getWikiSite().authority());
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

    public Cursor queryAll(SQLiteDatabase db) {
        return db.query(getTableName(), null, null, null, null, null, null);
    }

    @Override
    protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
    }

    @Override
    protected int getDBVersionDroppedAt() {
        return DB_VER_DROPPED;
    }

    @Override
    public SavedPage fromCursor(Cursor cursor) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ContentValues toContentValues(SavedPage obj) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPrimaryKeySelection(@NonNull SavedPage obj, @NonNull String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull SavedPage obj) {
        throw new UnsupportedOperationException();
    }
}
