package org.wikipedia.readinglist.page.database;

import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.async.AsyncTable;
import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.database.http.HttpRow;
import org.wikipedia.database.http.HttpStatus;
import org.wikipedia.readinglist.page.ReadingListPageRow;

public class ReadingListPageHttpTable
        extends AsyncTable<HttpStatus, ReadingListPageRow, HttpRow<ReadingListPageRow>> {
    private static final int DATABASE_VERSION = 12;

    public ReadingListPageHttpTable() {
        super(ReadingListPageContract.TABLE_HTTP, ReadingListPageContract.Http.URI,
                ReadingListPageContract.HTTP_COLS);
    }

    @Override public HttpRow<ReadingListPageRow> fromCursor(@NonNull Cursor cursor) {
        return ReadingListPageContract.HTTP_COLS.val(cursor);
    }

    @Override protected int getDBVersionIntroducedAt() {
        return DATABASE_VERSION;
    }
}
