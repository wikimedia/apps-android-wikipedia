package org.wikipedia.database.column;

import android.database.Cursor;

import androidx.annotation.NonNull;

import org.threeten.bp.Instant;

public class DateColumn extends Column<Instant> {
    public DateColumn(@NonNull String tbl, @NonNull String name, @NonNull String type) {
        super(tbl, name, type);
    }

    @Override
    public Instant val(@NonNull Cursor cursor) {
        return Instant.ofEpochMilli(getLong(cursor));
    }
}
