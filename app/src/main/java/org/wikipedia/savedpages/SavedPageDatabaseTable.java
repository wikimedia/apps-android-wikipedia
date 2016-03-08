package org.wikipedia.savedpages;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.BuildConfig;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.column.DateColumn;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.StrColumn;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SavedPageDatabaseTable extends DatabaseTable<SavedPage> {
    private static final int DB_VER_INTRODUCED = 4;
    private static final int DB_VER_NAMESPACE_ADDED = 6;
    private static final int DB_VER_LANG_ADDED = 10;

    public static class Col {
        public static final LongColumn ID = new LongColumn(BaseColumns._ID, "integer primary key");
        public static final StrColumn SITE = new StrColumn("site", "string");
        public static final StrColumn LANG = new StrColumn("lang", "text");
        public static final StrColumn TITLE = new StrColumn("title", "string");
        public static final StrColumn NAMESPACE = new StrColumn("namespace", "string");
        public static final DateColumn TIMESTAMP = new DateColumn("timestamp", "integer");

        public static final List<? extends Column<?>> ALL;
        public static final List<? extends Column<?>> CONTENT = Arrays.<Column<?>>asList(SITE, LANG,
                TITLE, NAMESPACE, TIMESTAMP);
        public static final String[] SELECTION = DbUtil.names(SITE, LANG, NAMESPACE, TITLE);
        static {
            List<Column<?>> all = new ArrayList<>();
            all.add(ID);
            all.addAll(CONTENT);
            ALL = Collections.unmodifiableList(all);
        }
    }

    public SavedPageDatabaseTable() {
        super(BuildConfig.SAVED_PAGES_TABLE);
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
    protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
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

                SavedPage obj;
                if (hasNamespace(db)) {
                    if (hasLang(db)) {
                        obj = fromCursor(cursor);
                    } else {
                        obj = fromPreLangCursor(cursor);
                    }
                } else {
                    obj = fromPreNamespaceCursor(cursor);
                }
                File newDir = new File(SavedPage.getSavedPagesDir() + "/" + obj.getTitle().getIdentifier());
                new File(SavedPage.getSavedPagesDir() + "/" + getSavedPageDir(obj, title)).renameTo(newDir);
            }
        }
        cursor.close();
    }

    private boolean hasNamespace(@NonNull SQLiteDatabase db) {
        return db.getVersion() >= DB_VER_NAMESPACE_ADDED;
    }

    private boolean hasLang(@NonNull SQLiteDatabase db) {
        return db.getVersion() >= DB_VER_LANG_ADDED;
    }

    private SavedPage fromPreNamespaceCursor(@NonNull Cursor cursor) {
        return fromPreNamespaceCursor(cursor, null, null);
    }

    private SavedPage fromPreLangCursor(@NonNull Cursor cursor) {
        return fromPreNamespaceCursor(cursor, Col.NAMESPACE.val(cursor), null);
    }

    private SavedPage  fromPreNamespaceCursor(@NonNull Cursor cursor, @Nullable String namespace,
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
                db.updateWithOnConflict(getTableName(), values, Col.ID.getName() + " = ?",
                        new String[]{id}, SQLiteDatabase.CONFLICT_REPLACE);
            }
        } finally {
            cursor.close();
        }
    }
}
