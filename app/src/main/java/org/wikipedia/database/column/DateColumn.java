package org.wikipedia.database.column;

import android.database.Cursor;

import java.util.Date;

import androidx.annotation.NonNull;

public class DateColumn extends Column<Date> {
    public DateColumn(@NonNull String tbl, @NonNull String name, @NonNull String type) {
        super(tbl, name, type);
    }

    @Override
    public Date val(@NonNull Cursor cursor) {
        return new Date(getLong(cursor));
    }
}
