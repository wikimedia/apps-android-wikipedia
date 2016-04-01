package org.wikipedia.database.column;

import android.database.Cursor;
import android.support.annotation.NonNull;

public class IntColumn extends Column<Integer> {
    @Deprecated public IntColumn(@NonNull String name, @NonNull String type) {
        super(name, type);
    }

    public IntColumn(@NonNull String tbl, @NonNull String name, @NonNull String type) {
        super(tbl, name, type);
    }

    @Override
    public Integer val(@NonNull Cursor cursor) {
        return getInt(cursor);
    }
}