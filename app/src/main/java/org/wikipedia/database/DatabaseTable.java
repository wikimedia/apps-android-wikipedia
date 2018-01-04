package org.wikipedia.database;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wikipedia.database.column.Column;
import org.wikipedia.util.ArrayUtils;
import org.wikipedia.util.log.L;

public abstract class DatabaseTable<T> {
    protected static final int INITIAL_DB_VERSION = 1;

    @NonNull private final String tableName;
    @NonNull private final Uri baseContentURI;

    public DatabaseTable(@NonNull String tableName, @NonNull Uri baseContentURI) {
        this.tableName = tableName;
        this.baseContentURI = baseContentURI;
    }

    public abstract T fromCursor(Cursor c);

    protected abstract ContentValues toContentValues(T obj);

    @NonNull public String getTableName() {
        return tableName;
    }

    @NonNull public Column<?>[] getColumnsAdded(int version) {
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
        return ArrayUtils.removeAllOccurences(getUnfilteredPrimaryKeySelectionArgs(obj), null);
    }

    /**
     * Override to provide full list of selection arguments, including those which may have null
     * values, for use in constructing the SQL query string.
     * @param obj Object from which selection args are to be derived.
     * @return Array of selection arguments (including null values).
     */
    protected abstract String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull T obj);

    protected abstract int getDBVersionIntroducedAt();

    protected int getDBVersionDroppedAt() {
        return 0;
    }

    public void upgradeSchema(@NonNull SQLiteDatabase db, int fromVersion, int toVersion) {
        if (fromVersion < getDBVersionIntroducedAt()) {
            createTables(db);
            onUpgradeSchema(db, fromVersion, getDBVersionIntroducedAt());
        }

        for (int ver = Math.max(getDBVersionIntroducedAt(), fromVersion) + 1; ver <= toVersion; ++ver) {
            L.i("ver=" + ver);

            if (ver == getDBVersionDroppedAt()) {
                dropTable(db);
                break;
            }

            for (Column<?> column : getColumnsAdded(ver)) {
                String alterTableString = "ALTER TABLE " + tableName + " ADD COLUMN " + column;
                L.i(alterTableString);
                db.execSQL(alterTableString);
            }

            onUpgradeSchema(db, ver - 1, ver);
        }
    }

    @NonNull public Uri getBaseContentURI() {
        return baseContentURI;
    }

    protected void onUpgradeSchema(@NonNull SQLiteDatabase db, int fromVersion, int toVersion) {
    }

    private void createTables(@NonNull SQLiteDatabase db) {
        L.i("Creating table=" + getTableName());
        Column<?>[] cols = getColumnsAdded(getDBVersionIntroducedAt());
        db.execSQL("CREATE TABLE " + getTableName() + " ( " + TextUtils.join(", ", cols) + " )");
    }

    private void dropTable(@NonNull SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + getTableName());
        L.i("Dropped table=" + getTableName());
    }
}
