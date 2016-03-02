package org.wikipedia.database.column;

import android.database.Cursor;
import android.support.annotation.NonNull;

public class Column<T> {
    @NonNull private final String name;
    @NonNull private final String type;

    /**
     * @param name Column name.
     * @param type SQLite datatype and constraints.
     */
    public Column(@NonNull String name, @NonNull String type) {
        this.name = name;
        this.type = type;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public T val(@NonNull Cursor cursor) {
        // TODO: update all subclasses and make this method abstract. Also update type params.
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return getName() + " " + getType();
    }

    protected int getInt(@NonNull Cursor cursor) {
        return cursor.getInt(getIndex(cursor));
    }

    protected long getLong(@NonNull Cursor cursor) {
        return cursor.getLong(getIndex(cursor));
    }

    protected String getString(@NonNull Cursor cursor) {
        return cursor.getString(getIndex(cursor));
    }

    protected int getIndex(@NonNull Cursor cursor) {
        return cursor.getColumnIndexOrThrow(getName());
    }
}