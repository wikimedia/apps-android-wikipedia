package org.wikipedia.useroption.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.contract.UserOptionContract;
import org.wikipedia.database.contract.UserOptionContract.Col;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserOptionDatabaseTable extends DatabaseTable<UserOptionRow> {
    private static final int INTRODUCED_AT_DATABASE_VERSION = 9;
    private static final List<? extends Column<?>> CONTENT_COLS;
    static {
        List<Column<?>> cols = new ArrayList<>();
        cols.add(Col.KEY);
        cols.add(Col.VAL);
        cols.addAll(Col.HTTP.all());
        CONTENT_COLS = Collections.unmodifiableList(cols);
    }

    public UserOptionDatabaseTable() {
        super(UserOptionContract.TABLE, UserOptionContract.Option.URI);
    }

    @Override
    public UserOptionRow fromCursor(Cursor cursor) {
        String key = Col.KEY.val(cursor);
        String val = Col.VAL.val(cursor);
        return new UserOptionRow(key, val, Col.HTTP.val(cursor));
    }

    @NonNull public Object[] toBindArgs(ContentValues values) {
        Object[] args = new Object[values.size()];
        for (int i = 0; i < values.size(); ++i) {
            args[i] = values.get(CONTENT_COLS.get(i).getName());
        }
        return args;
    }

    @NonNull
    @Override
    public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case INTRODUCED_AT_DATABASE_VERSION:
                List<Column<?>> cols = new ArrayList<>();
                cols.add(Col.ID);
                cols.add(Col.KEY);
                cols.add(Col.VAL);
                cols.addAll(Col.HTTP.all());
                return cols.toArray(new Column<?>[cols.size()]);
            default:
                return super.getColumnsAdded(version);
        }
    }

    @Override
    protected ContentValues toContentValues(UserOptionRow option) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Col.KEY.getName(), option.key());
        contentValues.put(Col.VAL.getName(), option.val());
        Col.HTTP.put(contentValues, option);
        return contentValues;
    }

    @Override
    protected String getPrimaryKeySelection(@NonNull UserOptionRow option,
                                            @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(option, Col.SELECTION);
    }

    @Override
    protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull UserOptionRow option) {
        return new String[] {option.key()};
    }

    @Override
    protected int getDBVersionIntroducedAt() {
        return INTRODUCED_AT_DATABASE_VERSION;
    }
}
