package org.wikipedia.database.column;

import android.database.Cursor;
import android.support.annotation.NonNull;

public class IntColumn extends Column<Integer> {
    public IntColumn(@NonNull String name, @NonNull String type) {
        super(name, type);
    }

    @Override
    public Integer val(@NonNull Cursor cursor) {
        return getInt(cursor);
    }
}