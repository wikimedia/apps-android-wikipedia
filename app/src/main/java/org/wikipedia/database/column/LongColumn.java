package org.wikipedia.database.column;

import android.database.Cursor;
import android.support.annotation.NonNull;

public class LongColumn extends Column<Long> {
    public LongColumn(@NonNull String name, @NonNull String type) {
        super(name, type);
    }

    public Long val(@NonNull Cursor cursor) {
        return getLong(cursor);
    }
}