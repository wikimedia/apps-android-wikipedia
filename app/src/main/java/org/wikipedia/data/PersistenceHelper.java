package org.wikipedia.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import static org.wikipedia.util.StringUtil.removeNulls;

public abstract class PersistenceHelper<T> {

    public static class Column{
        private final String name;
        private final String type;

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public Column(String name, String type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString() {
            return getName() + " " + getType();
        }
    }

    public abstract T fromCursor(Cursor c);

    protected abstract ContentValues toContentValues(T obj);

    public abstract String getTableName();

    public abstract Column[] getColumnsAdded(int version);

    /**
     * Get the db query string to be passed to the content provider where selecting for a null
     * value (including, notably, the main namespace) may be necessary.
     * @param obj The object on which the formatting of the string depends.
     * @return A SQL WHERE clause formatted for the content provider.
     */
    protected String getPrimaryKeySelection(T obj, String[] selectionKeys) {
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
    protected String[] getPrimaryKeySelectionArgs(T obj) {
        return removeNulls(getUnfilteredPrimaryKeySelectionArgs(obj));
    }

    /**
     * Override to provide full list of selection arguments, including those which may have null
     * values, for use in constructing the SQL query string.
     * @param obj Object from which selection args are to be derived.
     * @return Array of selection arguments (including null values).
     */
    protected abstract String[] getUnfilteredPrimaryKeySelectionArgs(T obj);

    protected int getDBVersionIntroducedAt() {
        return 1;
    }

    public ArrayList<Column> getElements(int fromVersion, int toVersion) {
         ArrayList<Column> columns = new ArrayList<>();
         for (int i = fromVersion; i <= toVersion; i++) {
             columns.addAll(Arrays.asList(getColumnsAdded(i)));
         }
         return columns;
     }

    public void createTables(SQLiteDatabase db, int version) {
        db.execSQL("CREATE TABLE " + getTableName() + " ( " + TextUtils.join(", ", getElements(1, version)) + " );");
    }

    public void upgradeSchema(SQLiteDatabase db, int fromVersion, int toVersion) {
        if (fromVersion < getDBVersionIntroducedAt()) {
            createTables(db, toVersion);
            return;
        }
        String tableName = getTableName();
        ArrayList<Column> columns = getElements(fromVersion + 1, toVersion);
        if (columns.size() == 0) {
            return;
        }
        ArrayList<String> columnCommands = new ArrayList<>(columns.size());
        for (Column column : columns) {
            columnCommands.add("ADD COLUMN " + column);
        }
        String alterTableString = "ALTER TABLE " + tableName + " " + TextUtils.join(", ", columnCommands) + ";";
        Log.d("Wikipedia", alterTableString);
        db.execSQL(alterTableString);
    }

    private Uri baseContentURI;
    public Uri getBaseContentURI() {
        if (baseContentURI == null) {
            baseContentURI = Uri.parse("content://" + SQLiteContentProvider.getAuthorityForTable(getTableName()) + "/" + getTableName());
        }
        return baseContentURI;
    }
}
