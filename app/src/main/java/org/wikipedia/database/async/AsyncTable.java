package org.wikipedia.database.async;

import android.content.ContentValues;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.model.EnumCode;

import java.util.ArrayList;
import java.util.List;

public abstract class AsyncTable<Status extends EnumCode, Dat, Row extends AsyncRow<Status, Dat>>
        extends DatabaseTable<Row> {
    @NonNull private final AsyncColumns<Status, Dat, Row> cols;

    public AsyncTable(@NonNull String tbl, @NonNull Uri baseUri,
                      @NonNull AsyncColumns<Status, Dat, Row> cols) {
        super(tbl, baseUri);
        this.cols = cols;
    }

    @NonNull @Override public Column<?>[] getColumnsAdded(int version) {
        if (version == getDBVersionIntroducedAt()) {
            List<Column<?>> added = new ArrayList<>();
            added.add(cols.id());
            added.add(cols.key());
            added.add(cols.status());
            added.add(cols.timestamp());
            added.add(cols.transactionId());
            return added.toArray(new Column<?>[added.size()]);
        }
        return super.getColumnsAdded(version);
    }

    @Override protected ContentValues toContentValues(@NonNull Row row) {
        return cols.toContentValues(row);
    }

    @Override protected String getPrimaryKeySelection(@NonNull Row row,
                                                      @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(row, new String[] {cols.key().getName()});
    }

    @Override protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull Row row) {
        return new String[] {row.key()};
    }
}
