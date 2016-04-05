package org.wikipedia.useroption.database;

import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.async.AsyncTable;
import org.wikipedia.database.contract.UserOptionContract;
import org.wikipedia.database.http.HttpRow;
import org.wikipedia.database.http.HttpStatus;

public class UserOptionHttpDatabaseTable extends AsyncTable<HttpStatus, HttpRow> {
    private static final int DATABASE_VERSION = 11;

    public UserOptionHttpDatabaseTable() {
        super(UserOptionContract.TABLE_HTTP, UserOptionContract.Http.URI, UserOptionContract.HTTP_COLS);
    }

    @Override public HttpRow fromCursor(@NonNull Cursor cursor) {
        return UserOptionContract.HTTP_COLS.val(cursor);
    }

    @Override protected int getDBVersionIntroducedAt() {
        return DATABASE_VERSION;
    }
}