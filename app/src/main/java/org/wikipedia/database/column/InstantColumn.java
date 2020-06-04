package org.wikipedia.database.column;

import android.database.Cursor;

import androidx.annotation.NonNull;

import java.time.Instant;

public class InstantColumn extends Column<Instant> {
    public InstantColumn(@NonNull String tbl, @NonNull String name, @NonNull String type) {
        super(tbl, name, type);
    }

    @Override
    public Instant val(@NonNull Cursor cursor) {
        return Instant.ofEpochMilli(getLong(cursor));
    }
}
