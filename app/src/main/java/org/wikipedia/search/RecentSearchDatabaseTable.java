package org.wikipedia.search;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.contract.SearchHistoryContract;
import org.wikipedia.database.contract.SearchHistoryContract.Col;

import java.util.Date;

public class RecentSearchDatabaseTable extends DatabaseTable<RecentSearch> {
    private static final int DB_VER_INTRODUCED = 5;

    public RecentSearchDatabaseTable() {
        super(SearchHistoryContract.TABLE, SearchHistoryContract.Query.URI);
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
    protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
    }

    @NonNull
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
