package org.wikipedia.database.column;

import android.database.Cursor;

import androidx.annotation.NonNull;

public class IntColumn extends Column<Integer> {
    public IntColumn(@NonNull String tbl, @NonNull String name, @NonNull String type) {
        super(tbl, name, type);
    }

    @Override
    public Integer value(@NonNull Cursor cursor) {
        return getInt(cursor);
    }
}
