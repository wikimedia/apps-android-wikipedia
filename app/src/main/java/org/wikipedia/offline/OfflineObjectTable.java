package org.wikipedia.offline;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.contract.OfflineObjectContract;

import java.util.ArrayList;
import java.util.List;

public final class OfflineObjectTable extends DatabaseTable<OfflineObject> {
    private static final int DB_VER_INTRODUCED = 20;
    public static final OfflineObjectTable DATABASE_TABLE = new OfflineObjectTable();

    private OfflineObjectTable() {
        super(OfflineObjectContract.TABLE, OfflineObjectContract.URI);
    }

    @Override public OfflineObject fromCursor(@NonNull Cursor cursor) {
        OfflineObject obj = new OfflineObject(OfflineObjectContract.Col.URL.value(cursor),
                OfflineObjectContract.Col.LANG.value(cursor),
                OfflineObjectContract.Col.PATH.value(cursor),
                OfflineObjectContract.Col.STATUS.value(cursor));
        String usedByStr = OfflineObjectContract.Col.USEDBY.value(cursor);
        if (!TextUtils.isEmpty(usedByStr)) {
            String[] usedBy = usedByStr.split("\\|");
            for (String s : usedBy) {
                if (!TextUtils.isEmpty(s)) {
                    obj.getUsedBy().add(Long.parseLong(s));
                }
            }
        }
        return obj;
    }

    @NonNull @Override public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case DB_VER_INTRODUCED:
                List<Column<?>> cols = new ArrayList<>();
                cols.add(OfflineObjectContract.Col.ID);
                cols.add(OfflineObjectContract.Col.URL);
                cols.add(OfflineObjectContract.Col.LANG);
                cols.add(OfflineObjectContract.Col.PATH);
                cols.add(OfflineObjectContract.Col.USEDBY);
                cols.add(OfflineObjectContract.Col.STATUS);
                return cols.toArray(new Column<?>[cols.size()]);
            default:
                return super.getColumnsAdded(version);
        }
    }

    @Override protected ContentValues toContentValues(@NonNull OfflineObject row) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(OfflineObjectContract.Col.URL.getName(), row.getUrl());
        contentValues.put(OfflineObjectContract.Col.LANG.getName(), row.getLang());
        contentValues.put(OfflineObjectContract.Col.PATH.getName(), row.getPath());
        contentValues.put(OfflineObjectContract.Col.STATUS.getName(), row.getStatus());
        contentValues.put(OfflineObjectContract.Col.USEDBY.getName(), '|' + StringUtils.join(row.getUsedBy(), '|') + '|');
        return contentValues;
    }

    @Override protected String getPrimaryKeySelection(@NonNull OfflineObject row,
                                                      @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(row, OfflineObjectContract.Col.SELECTION);
    }

    @Override protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull OfflineObject row) {
        return new String[] {row.getUrl()};
    }

    @Override protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
    }
}
