package org.wikipedia.search;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.column.DateColumn;
import org.wikipedia.database.column.LongColumn;
import org.wikipedia.database.column.StrColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class RecentSearchDatabaseTable extends DatabaseTable<RecentSearch> {
    private static final int DB_VER_INTRODUCED = 5;

    public static class Col {
        public static final LongColumn ID = new LongColumn(BaseColumns._ID, "integer primary key");
        public static final StrColumn TEXT = new StrColumn("text", "string");
        public static final DateColumn TIMESTAMP = new DateColumn("timestamp", "integer");

        public static final List<? extends Column<?>> ALL;
        public static final List<? extends Column<?>> CONTENT = Arrays.<Column<?>>asList(TEXT, TIMESTAMP);
        public static final String[] SELECTION = DbUtil.names(TEXT);
        static {
            List<Column<?>> all = new ArrayList<>();
            all.add(ID);
            all.addAll(CONTENT);
            ALL = Collections.unmodifiableList(all);
        }
    }

    @Override
    public RecentSearch fromCursor(Cursor cursor) {
        String title = Col.TEXT.val(cursor);
        Date timestamp = Col.TIMESTAMP.val(cursor);
        return new RecentSearch(title, timestamp);
    }

    @Override
    protected ContentValues toContentValues(RecentSearch obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Col.TEXT.getName(), obj.getText());
        contentValues.put(Col.TIMESTAMP.getName(), obj.getTimestamp().getTime());
        return contentValues;
    }

    @Override
    public String getTableName() {
        return "recentsearches";
    }

    @Override
    protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
    }

    @Override
    public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case DB_VER_INTRODUCED:
                return new Column<?>[] {Col.ID, Col.TEXT, Col.TIMESTAMP};
            default:
                return super.getColumnsAdded(version);
        }
    }

    @Override
    protected String getPrimaryKeySelection(@NonNull RecentSearch obj, @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(obj, Col.SELECTION);
    }

    @Override
    protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull RecentSearch obj) {
        return new String[] {obj.getText()};
    }
}
