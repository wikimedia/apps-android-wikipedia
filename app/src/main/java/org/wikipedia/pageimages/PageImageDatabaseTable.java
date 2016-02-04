package org.wikipedia.pageimages;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.database.DatabaseTable;

public class PageImageDatabaseTable extends DatabaseTable<PageImage> {

    private static final int DB_VER_NAMESPACE_ADDED = 7;

    private static final String COL_SITE = "site";
    private static final String COL_NAMESPACE = "namespace";
    private static final String COL_TITLE = "title";
    private static final String COL_IMAGE_NAME = "imageName";

    public static final String[] SELECTION_KEYS = {
            COL_SITE,
            COL_NAMESPACE,
            COL_TITLE
    };

    @Override
    public PageImage fromCursor(Cursor c) {
        Site site = new Site(c.getString(c.getColumnIndex(COL_SITE)));
        PageTitle title = new PageTitle(c.getString(c.getColumnIndex(COL_NAMESPACE)), c.getString(c.getColumnIndex(COL_TITLE)), site);
        String imageName = c.getString(c.getColumnIndex(COL_IMAGE_NAME));
        return new PageImage(title, imageName);
    }

    @Override
    protected ContentValues toContentValues(PageImage obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_SITE, obj.getTitle().getSite().getDomain());
        contentValues.put(COL_NAMESPACE, obj.getTitle().getNamespace());
        contentValues.put(COL_TITLE, obj.getTitle().getPrefixedText());
        contentValues.put(COL_IMAGE_NAME, obj.getImageName());
        return contentValues;
    }

    @Nullable
    public String getImageUrlForTitle(WikipediaApp app, PageTitle title) {
        Cursor c = null;
        String thumbnail = null;
        try {
            String searchStr = title.getPrefixedText().replace("'", "''");
            String selection = getTableName() + "." + COL_TITLE + "='" + searchStr + "'";
            c = app.getDatabaseClient(PageImage.class).select(
                    selection, new String[] {}, "");
            if (c.getCount() > 0) {
                c.moveToFirst();
                thumbnail = c.getString(c.getColumnIndex("imageName"));
            }
        } catch (SQLiteException e) {
            // page title doesn't exist in database... no problem if it fails.
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return thumbnail;
    }

    @Override
    public String getTableName() {
        return "pageimages";
    }

    @Override
    public Column[] getColumnsAdded(int version) {
        switch (version) {
            case 1:
                return new Column[] {
                        new Column("_id", "integer primary key"),
                        new Column(COL_SITE, "string"),
                        new Column(COL_TITLE, "string"),
                        new Column(COL_IMAGE_NAME, "string"),
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
    protected String getPrimaryKeySelection(@NonNull PageImage obj, @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(obj, SELECTION_KEYS);
    }

    @Override
    protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull PageImage obj) {
        return new String[] {
                obj.getTitle().getSite().getDomain(),
                obj.getTitle().getNamespace(),
                obj.getTitle().getText()
        };
    }

    @Override
    protected int getDBVersionIntroducedAt() {
        return INITIAL_DB_VERSION;
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
            }
        }
        cursor.close();
    }
}
