package org.wikipedia.pageimages;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.StrColumn;
import org.wikipedia.page.PageTitle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PageImageDatabaseTable extends DatabaseTable<PageImage> {
    private static final int DB_VER_NAMESPACE_ADDED = 7;
    private static final int DB_VER_LANG_ADDED = 10;

    public static class Col {
        public static final LongColumn ID = new LongColumn(BaseColumns._ID, "integer primary key");
        public static final StrColumn SITE = new StrColumn("site", "string");
        public static final StrColumn LANG = new StrColumn("lang", "text");
        public static final StrColumn TITLE = new StrColumn("title", "string");
        public static final StrColumn NAMESPACE = new StrColumn("namespace", "string");
        public static final StrColumn IMAGE_NAME = new StrColumn("imageName", "string");

        public static final List<? extends Column<?>> ALL;
        public static final List<? extends Column<?>> CONTENT = Arrays.<Column<?>>asList(SITE, LANG,
                TITLE, NAMESPACE, IMAGE_NAME);
        public static final String[] SELECTION = DbUtil.names(SITE, LANG, NAMESPACE, TITLE);
        static {
            List<Column<?>> all = new ArrayList<>();
            all.add(ID);
            all.addAll(CONTENT);
            ALL = Collections.unmodifiableList(all);
        }
    }

    @Override
    public PageImage fromCursor(Cursor cursor) {
        Site site = new Site(Col.SITE.val(cursor), Col.LANG.val(cursor));
        PageTitle title = new PageTitle(Col.NAMESPACE.val(cursor), Col.TITLE.val(cursor), site);
        String imageName = Col.IMAGE_NAME.val(cursor);
        return new PageImage(title, imageName);
    }

    @Override
    protected ContentValues toContentValues(PageImage obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Col.SITE.getName(), obj.getTitle().getSite().authority());
        contentValues.put(Col.LANG.getName(), obj.getTitle().getSite().languageCode());
        contentValues.put(Col.NAMESPACE.getName(), obj.getTitle().getNamespace());
        contentValues.put(Col.TITLE.getName(), obj.getTitle().getPrefixedText());
        contentValues.put(Col.IMAGE_NAME.getName(), obj.getImageName());
        return contentValues;
    }

    @Nullable
    public String getImageUrlForTitle(WikipediaApp app, PageTitle title) {
        Cursor c = null;
        String thumbnail = null;
        try {
            String searchStr = title.getPrefixedText().replace("'", "''");
            String selection = getTableName() + "." + Col.TITLE.getName() + "='" + searchStr + "'";
            c = app.getDatabaseClient(PageImage.class).select(
                    selection, new String[] {}, "");
            if (c.getCount() > 0) {
                c.moveToFirst();
                thumbnail = getString(c, Col.IMAGE_NAME.getName());
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

    public String getImageColumnName() {
        return Col.IMAGE_NAME.getName();
    }

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
                obj.getTitle().getSite().authority(),
                obj.getTitle().getSite().languageCode(),
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

    @Override
    protected void addLangToAllSites(@NonNull SQLiteDatabase db) {
        super.addLangToAllSites(db);
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
