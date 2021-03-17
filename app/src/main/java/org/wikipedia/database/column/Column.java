package org.wikipedia.database.column;

import android.database.Cursor;

import androidx.annotation.NonNull;

public abstract class Column<T> {
    private final String tbl;
    @NonNull private final String name;
    @NonNull private final String type;

    /**
     * @param name Column name.
     * @param type SQLite datatype and constraints.
     */
    public Column(@NonNull String tbl, @NonNull String name, @NonNull String type) {
        this.tbl = tbl;
        this.name = name;
        this.type = type;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull public String qualifiedName() {
        return tbl + '.' + name;
    }

    @NonNull
    public String getType() {
        return type;
    }

    // TODO: add ContentValues marshalling method and hide getName().
    public abstract T value(@NonNull Cursor cursor);

    // TODO: it would be more convenient in more places to just return the qualified name. It's
    //       easy to accidentally implicitly convert a Column to a String with `+ ""` and it's only
    //       appropriate to do so when creating or altering tables which occurs less frequently than
    //       actually using them.
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
