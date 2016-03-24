package org.wikipedia.database.column;

import android.database.Cursor;
import android.support.annotation.NonNull;

public class StrColumn extends Column<String> {
    public StrColumn(@NonNull String name, @NonNull String type) {
        super(name, type);
    }

    @Override
    public String val(@NonNull Cursor cursor) {
        return getString(cursor);
    }
}