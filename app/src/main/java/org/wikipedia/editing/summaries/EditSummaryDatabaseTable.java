package org.wikipedia.editing.summaries;

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

public class EditSummaryDatabaseTable extends DatabaseTable<EditSummary> {
    private static final int DB_VER_INTRODUCED = 2;

    public static class Col {
        public static final LongColumn ID = new LongColumn(BaseColumns._ID, "integer primary key");
        public static final StrColumn SUMMARY = new StrColumn("summary", "string");
        public static final DateColumn LAST_USED = new DateColumn("lastUsed", "integer");

        public static final List<? extends Column<?>> ALL;
        public static final List<? extends Column<?>> CONTENT = Arrays.<Column<?>>asList(SUMMARY, LAST_USED);
        public static final String[] SELECTION = DbUtil.names(SUMMARY);
        static {
            List<Column<?>> all = new ArrayList<>();
            all.add(ID);
            all.addAll(CONTENT);
            ALL = Collections.unmodifiableList(all);
        }
    }

    @Override
    public EditSummary fromCursor(Cursor cursor) {
        String summary = Col.SUMMARY.val(cursor);
        Date lastUsed = Col.LAST_USED.val(cursor);
        return new EditSummary(summary, lastUsed);
    }

    @Override
    protected ContentValues toContentValues(EditSummary obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Col.SUMMARY.getName(), obj.getSummary());
        contentValues.put(Col.LAST_USED.getName(), obj.getLastUsed().getTime());
        return contentValues;
    }

    @Override
    public String getTableName() {
        return "editsummaries";
    }

    @Override
    protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
    }

    @Override
    public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case DB_VER_INTRODUCED:
                return new Column<?>[] {Col.ID, Col.SUMMARY, Col.LAST_USED};
            default:
                return super.getColumnsAdded(version);
        }
    }

    @Override
    protected String getPrimaryKeySelection(@NonNull EditSummary obj, @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(obj, Col.SELECTION);
    }

    @Override
    protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull EditSummary obj) {
        return new String[] {
                obj.getSummary()
        };
    }
}
