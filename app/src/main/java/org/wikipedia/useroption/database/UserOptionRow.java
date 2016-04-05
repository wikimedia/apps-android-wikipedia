package org.wikipedia.useroption.database;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.contract.UserOptionContract;
import org.wikipedia.database.http.HttpRow;
import org.wikipedia.useroption.UserOption;

public class UserOptionRow extends HttpRow {
    public static final UserOptionDatabaseTable DATABASE_TABLE = new UserOptionDatabaseTable();
    public static final UserOptionHttpDatabaseTable HTTP_DATABASE_TABLE = new UserOptionHttpDatabaseTable();

    @Nullable private UserOption option;

    public static UserOptionRow fromCursor(@NonNull Cursor cursor) {
        HttpRow httpRow = HTTP_DATABASE_TABLE.fromCursor(cursor);
        boolean hasOption = cursor.getColumnIndex(UserOptionContract.OptionWithHttp.KEY.getName()) != -1;
        UserOption option = hasOption ? DATABASE_TABLE.fromCursor(cursor) : null;
        return new UserOptionRow(httpRow, option);
    }

    public UserOptionRow(@NonNull UserOption option) {
        super(option.key());
        this.option = option;
    }

    public UserOptionRow(@NonNull HttpRow httpRow, @Nullable UserOption option) {
        super(httpRow.key(), httpRow.status(), httpRow.timestamp(), httpRow.transactionId());
        this.option = option;
    }

    @Nullable public UserOption option() {
        return option;
    }
}
