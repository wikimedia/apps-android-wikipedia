package org.wikipedia.useroption.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.BuildConfig;
import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.column.IdColumn;
import org.wikipedia.database.column.StrColumn;
import org.wikipedia.database.http.HttpColumns;
import org.wikipedia.database.http.HttpStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserOptionDatabaseTable extends DatabaseTable<UserOptionRow> {
    public static final class Col {
        public static final IdColumn ID = new IdColumn();
        public static final StrColumn KEY = new StrColumn("key", "text not null unique");
        public static final StrColumn VAL = new StrColumn("val", "text");
        public static final HttpColumns HTTP = new HttpColumns("sync");

        public static final List<? extends Column<?>> ALL;
        public static final List<? extends Column<?>> CONTENT;
        public static final String SELECTION = KEY.getName();
        static {
            List<Column<?>> content = new ArrayList<>();
            content.add(KEY);
            content.add(VAL);
            content.addAll(HTTP.all());
            CONTENT = Collections.unmodifiableList(content);

            List<Column<?>> all = new ArrayList<>();
            all.add(ID);
            all.addAll(content);
            ALL = Collections.unmodifiableList(all);
        }
    }

    private static final int INTRODUCED_AT_DATABASE_VERSION = 9;

    public UserOptionDatabaseTable() {
        super(BuildConfig.USER_OPTION_TABLE);
    }

    @Override
    public UserOptionRow fromCursor(Cursor cursor) {
        String key = Col.KEY.val(cursor);
        String val = Col.VAL.val(cursor);
        HttpStatus status = Col.HTTP.status(cursor);
        long timestamp = Col.HTTP.timestamp(cursor);
        long transactionId = Col.HTTP.transactionId(cursor);
        return new UserOptionRow(key, val, status, timestamp, transactionId);
    }

    @NonNull public Object[] toBindArgs(ContentValues values) {
        Object[] args = new Object[values.size()];
        for (int i = 0; i < values.size(); ++i) {
            args[i] = values.get(Col.CONTENT.get(i).getName());
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
        return super.getPrimaryKeySelection(option, new String[] {Col.SELECTION});
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
