package org.wikipedia.readinglist.page.database;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.database.http.HttpRow;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.ReadingListPageRow;

public class ReadingListPageHttpRow extends HttpRow<ReadingListPageRow> {
    public static ReadingListPageHttpRow fromCursor(@NonNull Cursor cursor) {
        HttpRow<ReadingListPageRow> httpRow = ReadingListPage.HTTP_DATABASE_TABLE.fromCursor(cursor);
        boolean hasRow = ReadingListPageContract.HttpWithPage.KEY.val(cursor) != null;
        ReadingListPageRow row = hasRow ? ReadingListPage.DATABASE_TABLE.fromCursor(cursor) : null;
        return new ReadingListPageHttpRow(httpRow, row);
    }

    public ReadingListPageHttpRow(@NonNull ReadingListPage row) {
        super(row.key(), row);
    }

    public ReadingListPageHttpRow(@NonNull HttpRow<ReadingListPageRow> httpRow,
                                  @Nullable ReadingListPageRow row) {
        super(httpRow, row);
    }
}
