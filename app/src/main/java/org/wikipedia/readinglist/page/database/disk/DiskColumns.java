package org.wikipedia.readinglist.page.database.disk;

import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.async.AsyncColumns;

public class DiskColumns<T> extends AsyncColumns<DiskStatus, T, DiskRow<T>> {
    public DiskColumns(@NonNull String tbl) {
        super(tbl, "disk", DiskStatus.CODE_ENUM);
    }

    @NonNull @Override public DiskRow<T> val(@NonNull Cursor cursor) {
        return new DiskRow<>(key(cursor), status(cursor), timestamp(cursor), transactionId(cursor));
    }
}
