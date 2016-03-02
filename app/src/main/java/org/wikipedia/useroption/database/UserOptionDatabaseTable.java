package org.wikipedia.useroption.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.BuildConfig;
import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.column.IdColumn;
import org.wikipedia.database.column.StrColumn;
import org.wikipedia.database.sync.SyncColumn;
import org.wikipedia.database.sync.SyncStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserOptionDatabaseTable extends DatabaseTable<UserOptionRow> {
    public static final class Col implements SyncColumn {
        public static final IdColumn ID = new IdColumn();
        public static final StrColumn KEY = new StrColumn("key", "text not null unique");
        public static final StrColumn VAL = new StrColumn("val", "text");

        public static final List<? extends Column<?>> ALL;
        public static final List<? extends Column<?>> CONTENT;
        public static final Column<?> SELECTION = KEY;
        static {
            List<Column<?>> content = new ArrayList<>();
            content.add(KEY);
            content.add(VAL);
            content.addAll(SyncColumn.ALL);
            CONTENT = Collections.unmodifiableList(content);

            List<Column<?>> all = new ArrayList<>();
            all.add(ID);
            all.addAll(content);
            ALL = Collections.unmodifiableList(all);
        }
    }

    private static final int INTRODUCED_AT_DATABASE_VERSION = 9;

    @Override
    public UserOptionRow fromCursor(Cursor cursor) {
        String key = Col.KEY.val(cursor);
        String val = Col.VAL.val(cursor);
        SyncStatus status = Col.SYNC_STATUS.val(cursor);
        long transactionId = Col.SYNC_TRANSACTION_ID.val(cursor);
        long timestamp = Col.SYNC_TIMESTAMP.val(cursor);
        return new UserOptionRow(key, val, status, transactionId, timestamp);
    }

    @NonNull public Object[] toBindArgs(ContentValues values) {
        Object[] args = new Object[values.size()];
        for (int i = 0; i < values.size(); ++i) {
            args[i] = values.get(Col.CONTENT.get(i).getName());
        }
        return args;
    }

    @Override
    public String getTableName() {
        return BuildConfig.USER_OPTION_TABLE;
    }

    @Override
    public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case INTRODUCED_AT_DATABASE_VERSION:
                return new Column<?>[] {Col.ID, Col.KEY, Col.VAL, Col.SYNC_STATUS,
                        Col.SYNC_TRANSACTION_ID, SyncColumn.SYNC_TIMESTAMP};
            default:
                return new Column[0];
        }
    }

    @Override
    protected ContentValues toContentValues(UserOptionRow option) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Col.KEY.getName(), option.key());
        contentValues.put(Col.VAL.getName(), option.val());
        contentValues.put(Col.SYNC_STATUS.getName(), option.status().code());
        contentValues.put(Col.SYNC_TRANSACTION_ID.getName(), option.transactionId());
        contentValues.put(Col.SYNC_TIMESTAMP.getName(), option.timestamp());
        return contentValues;
    }

    @Override
    protected String getPrimaryKeySelection(@NonNull UserOptionRow option,
                                            @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(option, new String[] {Col.SELECTION.getName()});
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
