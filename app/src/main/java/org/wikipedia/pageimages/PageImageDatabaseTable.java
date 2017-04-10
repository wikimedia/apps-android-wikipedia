package org.wikipedia.pageimages;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.contract.PageImageHistoryContract;
import org.wikipedia.database.contract.PageImageHistoryContract.Col;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.log.L;


// todo: network caching preserves images. Remove this class and drop table?
public class PageImageDatabaseTable extends DatabaseTable<PageImage> {
    private static final int DB_VER_NAMESPACE_ADDED = 7;
    private static final int DB_VER_NORMALIZED_TITLES = 8;
    private static final int DB_VER_LANG_ADDED = 10;

    public PageImageDatabaseTable() {
        super(PageImageHistoryContract.TABLE, PageImageHistoryContract.Image.URI);
    }

    @Override
    public PageImage fromCursor(Cursor cursor) {
        WikiSite wiki = new WikiSite(Col.SITE.val(cursor), Col.LANG.val(cursor));
        PageTitle title = new PageTitle(Col.NAMESPACE.val(cursor), Col.TITLE.val(cursor), wiki);
        String imageName = Col.IMAGE_NAME.val(cursor);
        return new PageImage(title, imageName);
    }

    @Override
    protected ContentValues toContentValues(PageImage obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Col.SITE.getName(), obj.getTitle().getWikiSite().authority());
        contentValues.put(Col.LANG.getName(), obj.getTitle().getWikiSite().languageCode());
        contentValues.put(Col.NAMESPACE.getName(), obj.getTitle().getNamespace());
        contentValues.put(Col.TITLE.getName(), obj.getTitle().getPrefixedText());
        contentValues.put(Col.IMAGE_NAME.getName(), obj.getImageName());
        return contentValues;
    }

    @NonNull
    @Override
    public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case INITIAL_DB_VERSION:
                return new Column<?>[] {Col.ID, Col.SITE, Col.TITLE, Col.IMAGE_NAME};
            case DB_VER_NAMESPACE_ADDED:
                return new Column<?>[] {Col.NAMESPACE};
            case DB_VER_LANG_ADDED:
                return new Column<?>[] {Col.LANG};
            default:
                return super.getColumnsAdded(version);
        }
    }

    @Override
    protected String getPrimaryKeySelection(@NonNull PageImage obj, @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(obj, Col.SELECTION);
    }

    @Override
    protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull PageImage obj) {
        return new String[] {
                obj.getTitle().getWikiSite().authority(),
                obj.getTitle().getWikiSite().languageCode(),
                obj.getTitle().getNamespace(),
                obj.getTitle().getText()
        };
    }

    @Override
    protected int getDBVersionIntroducedAt() {
        return INITIAL_DB_VERSION;
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
            }
        }
        cursor.close();
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
                db.updateWithOnConflict(getTableName(), values, "_id = ?", new String[]{id}, SQLiteDatabase.CONFLICT_REPLACE);
            }
        } finally {
            cursor.close();
        }
    }
}
