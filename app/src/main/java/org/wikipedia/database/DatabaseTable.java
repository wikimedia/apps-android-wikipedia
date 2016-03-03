package org.wikipedia.database;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wikipedia.database.column.Column;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.wikipedia.util.StringUtil.removeNulls;

public abstract class DatabaseTable<T> {
    protected static final int INITIAL_DB_VERSION = 1;
    private static final int MIN_VERSION_NORMALIZED_TITLES = 8;
    private static final int MIN_VERSION_NORMALIZED_LANGS = 10;

    @NonNull private final String tableName;
    @NonNull private final Uri baseContentURI;

    public DatabaseTable(@NonNull String tableName) {
        this.tableName = tableName;
        baseContentURI = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SQLiteContentProvider.getAuthorityForTable(tableName))
                .path(tableName)
                .build();
    }

    public abstract T fromCursor(Cursor c);

    protected abstract ContentValues toContentValues(T obj);

    @NonNull
    public String getTableName() {
        return tableName;
    }

    @NonNull
    public Column<?>[] getColumnsAdded(int version) {
        return new Column<?>[0];
    }

    public ContentProviderClient acquireClient(@NonNull Context context) {
        return context.getContentResolver().acquireContentProviderClient(getBaseContentURI());
    }

    /**
     * Get the db query string to be passed to the content provider where selecting for a null
     * value (including, notably, the main namespace) may be necessary.
     * @param obj The object on which the formatting of the string depends.
     * @return A SQL WHERE clause formatted for the content provider.
     */
    protected String getPrimaryKeySelection(@NonNull T obj, @NonNull String[] selectionKeys) {
        String primaryKeySelection = "";
        String[] args = getUnfilteredPrimaryKeySelectionArgs(obj);
        for (int i = 0; i < args.length; i++) {
            primaryKeySelection += (selectionKeys[i] + (args[i] == null ? " IS NULL" : " = ?"));
            if (i < (args.length - 1)) {
                primaryKeySelection += " AND ";
            }
        }
        return primaryKeySelection;
    }

    /**
     * Get the selection arguments to be bound to the db query string.
     * @param obj The object from which selection args are derived.
     * @return The array of selection arguments with null values removed.  (Null arguments are
     * replaced with "IS NULL" in getPrimaryKeySelection(T obj, String[] selectionKeys).)
     */
    public String[] getPrimaryKeySelectionArgs(@NonNull T obj) {
        return removeNulls(getUnfilteredPrimaryKeySelectionArgs(obj));
    }

    /**
     * Override to provide full list of selection arguments, including those which may have null
     * values, for use in constructing the SQL query string.
     * @param obj Object from which selection args are to be derived.
     * @return Array of selection arguments (including null values).
     */
    protected abstract String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull T obj);

    protected abstract int getDBVersionIntroducedAt();

    public List<? extends Column<?>> getElements(int fromVersion, int toVersion) {
        List<Column<?>> columns = new ArrayList<>();
        for (int i = fromVersion; i <= toVersion; i++) {
            columns.addAll(Arrays.asList(getColumnsAdded(i)));
        }
        return columns;
    }

    public void createTables(@NonNull SQLiteDatabase db, int version) {
        L.i("Creating table=" + getTableName());
        db.execSQL("CREATE TABLE " + getTableName() + " ( " + TextUtils.join(", ", getElements(1, version)) + " );");
    }

    public void upgradeSchema(@NonNull SQLiteDatabase db, int fromVersion, int toVersion) {
        if (fromVersion < getDBVersionIntroducedAt()) {
            createTables(db, toVersion);
            return;
        }
        if (fromVersion < MIN_VERSION_NORMALIZED_TITLES) {
            convertAllTitlesToUnderscores(db);
        }
        List<? extends Column<?>> columns = getElements(fromVersion + 1, toVersion);
        if (columns.size() == 0) {
            return;
        }
        for (Column<?> column : columns) {
            String alterTableString = "ALTER TABLE " + tableName + " ADD COLUMN " + column + ";";
            L.d(alterTableString);
            db.execSQL(alterTableString);
        }
        if (fromVersion < MIN_VERSION_NORMALIZED_LANGS) {
            addLangToAllSites(db);
        }
    }

    @NonNull
    public Uri getBaseContentURI() {
        return baseContentURI;
    }

    /**
     * One-time fix for the inconsistencies in title formats all over the database. This migration will enforce
     * all titles stored in the database to follow the "Underscore_format" instead of the "Human readable form"
     * TODO: Delete this code after April 2016
     *
     * @param db Database object
     */
    protected void convertAllTitlesToUnderscores(SQLiteDatabase db) {
        // Default implementation is empty, since not every table needs to deal with titles
    }

    // TODO: remove in September 2016.
    protected void addLangToAllSites(@NonNull SQLiteDatabase db) {
        L.d("Adding language codes to " + getTableName());
    }
}
