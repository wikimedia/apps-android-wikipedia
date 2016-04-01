package org.wikipedia.database.column;

import android.database.Cursor;
import android.support.annotation.NonNull;

import java.util.Date;

public class DateColumn extends Column<Date> {
    @Deprecated public DateColumn(@NonNull String name, @NonNull String type) {
        super(name, type);
    }

    public DateColumn(@NonNull String tbl, @NonNull String name, @NonNull String type) {
        super(tbl, name, type);
    }

    @Override
    public Date val(@NonNull Cursor cursor) {
        return new Date(getLong(cursor));
    }
}