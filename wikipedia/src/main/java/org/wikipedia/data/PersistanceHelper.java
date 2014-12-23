package org.wikipedia.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class PersistanceHelper<T> {

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

    protected abstract String getPrimaryKeySelection();
    protected abstract String[] getPrimaryKeySelectionArgs(T obj);

    protected int getDBVersionIntroducedAt() {
        return 1;
    }

    public ArrayList<Column> getElements(int fromVersion, int toVersion) {
         ArrayList<Column> columns = new ArrayList<Column>();
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
        ArrayList<String> columnCommands = new ArrayList<String>(columns.size());
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
