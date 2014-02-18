package org.wikipedia.data;

import android.content.*;
import android.database.*;
import android.net.*;
import android.text.*;

import java.util.*;

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

    abstract public T fromCursor(Cursor c);

    abstract protected ContentValues toContentValues(T obj);

    abstract public String getTableName();

    abstract public Column[] getColumnsAdded(int version);

    protected abstract String getPrimaryKeySelection();
    protected abstract String[] getPrimaryKeySelectionArgs(T obj);

    public ArrayList<Column> getElements(int version) {
         ArrayList<Column> columns = new ArrayList<Column>();
         for (int i = 1; i <= version; i++) {
             columns.addAll(Arrays.asList(getColumnsAdded(i)));
         }
         return columns;
     }

    public String getSchema(int version) {
        String tableName = getTableName();
        ArrayList<Column> columns = getElements(version);
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE ").append(tableName).append(" ( ")
                .append(TextUtils.join(", ", columns))
                .append(" );");

        return builder.toString();
    }

    private Uri baseContentURI;
    public Uri getBaseContentURI() {
        if (baseContentURI == null) {
            baseContentURI = Uri.parse("content://" + SQLiteContentProvider.getAuthorityForTable(getTableName()) + "/" + getTableName());
        }
        return baseContentURI;
    }
}
