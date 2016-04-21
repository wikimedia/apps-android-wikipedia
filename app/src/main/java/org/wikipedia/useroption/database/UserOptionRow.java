package org.wikipedia.useroption.database;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.contract.UserOptionContract;
import org.wikipedia.database.http.HttpRow;
import org.wikipedia.useroption.UserOption;

public class UserOptionRow extends HttpRow<UserOption> {
    public static final UserOptionDatabaseTable DATABASE_TABLE = new UserOptionDatabaseTable();
    public static final UserOptionHttpDatabaseTable HTTP_DATABASE_TABLE = new UserOptionHttpDatabaseTable();

    public static UserOptionRow fromCursor(@NonNull Cursor cursor) {
        HttpRow<UserOption> httpRow = HTTP_DATABASE_TABLE.fromCursor(cursor);
        boolean hasRow = UserOptionContract.HttpWithOption.KEY.val(cursor) != null;
        UserOption row = hasRow ? DATABASE_TABLE.fromCursor(cursor) : null;
        return new UserOptionRow(httpRow, row);
    }

    public UserOptionRow(@NonNull UserOption row) {
        super(row.key(), row);
    }

    public UserOptionRow(@NonNull HttpRow<UserOption> httpRow, @Nullable UserOption row) {
        super(httpRow, row);
    }
}
