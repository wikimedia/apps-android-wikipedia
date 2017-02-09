package org.wikipedia.useroption.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.DbUtil;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.contract.UserOptionContract;
import org.wikipedia.database.contract.UserOptionContract.OptionCol;
import org.wikipedia.useroption.UserOption;
import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserOptionDatabaseTable extends DatabaseTable<UserOption> {
    private static final int INTRODUCED_AT_DATABASE_VERSION = 9;
    private static final int HTTP_COLS_REMOVED_AT_DATABASE_VERSION = 11;

    public UserOptionDatabaseTable() {
        super(UserOptionContract.TABLE_OPTION, UserOptionContract.Option.URI);
    }

    @Override
    public UserOption fromCursor(Cursor cursor) {
        String key = OptionCol.KEY.val(cursor);
        String val = OptionCol.VAL.val(cursor);
        return new UserOption(key, val);
    }

    @SuppressWarnings("deprecation")
    @NonNull
    @Override
    public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case INTRODUCED_AT_DATABASE_VERSION:
                List<Column<?>> cols = new ArrayList<>();
                cols.add(OptionCol.ID);
                cols.add(OptionCol.KEY);
                cols.add(OptionCol.VAL);
                cols.add(OptionCol.Legacy.HTTP_STATUS);
                cols.add(OptionCol.Legacy.HTTP_TIMESTAMP);
                cols.add(OptionCol.Legacy.HTTP_TRANSACTION_ID);
                return cols.toArray(new Column<?>[cols.size()]);
            default:
                return super.getColumnsAdded(version);
        }
    }

    @Override
    protected ContentValues toContentValues(UserOption option) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(OptionCol.KEY.getName(), option.key());
        contentValues.put(OptionCol.VAL.getName(), option.val());
        return contentValues;
    }

    @Override
    protected String getPrimaryKeySelection(@NonNull UserOption option,
                                            @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(option, OptionCol.SELECTION);
    }

    @Override
    protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull UserOption option) {
        return new String[] {option.key()};
    }

    @Override
    protected int getDBVersionIntroducedAt() {
        return INTRODUCED_AT_DATABASE_VERSION;
    }

    @Override protected void upgradeSchema(@NonNull SQLiteDatabase db, int toVersion) {
        switch (toVersion) {
            case HTTP_COLS_REMOVED_AT_DATABASE_VERSION:
                upgradeSchemaHttpColsRemoved(db);
                break;
            default:
                super.upgradeSchema(db, toVersion);
        }
    }

    private void upgradeSchemaHttpColsRemoved(@NonNull SQLiteDatabase db) {
        String httpCols = DbUtil.namesCsv(UserOptionContract.HttpCol.KEY,
                UserOptionContract.HttpCol.STATUS, UserOptionContract.HttpCol.TIMESTAMP,
                UserOptionContract.HttpCol.TRANSACTION_ID);
        @SuppressWarnings("deprecation") String oldHttpCols = DbUtil.namesCsv(OptionCol.KEY,
                OptionCol.Legacy.HTTP_STATUS, OptionCol.Legacy.HTTP_TIMESTAMP,
                OptionCol.Legacy.HTTP_TRANSACTION_ID);

        String colDefs = StringUtil.listToCsv(Arrays.asList(OptionCol.ID.toString(), OptionCol.KEY.toString(),
                OptionCol.VAL.toString()));
        String cols = DbUtil.namesCsv(OptionCol.ID, OptionCol.KEY, OptionCol.VAL);

        String sql = ("insert into :httpTbl (:httpCols) select :oldHttpCols from :tbl;"
                   +  "alter table :tbl rename to :tblBackup;"
                   +  "create table :tbl (:colDefs);"
                   +  "insert into :tbl select :cols from :tblBackup;"
                   +  "drop table :tblBackup")
                .replaceAll(":httpTbl", UserOptionContract.TABLE_HTTP)
                .replaceAll(":httpCols", httpCols)
                .replaceAll(":oldHttpCols", oldHttpCols)
                .replaceAll(":tbl", UserOptionContract.TABLE_OPTION)
                .replaceAll(":colDefs", colDefs)
                .replaceAll(":cols", cols);
        DbUtil.execSqlTransaction(db, sql);
    }
}
