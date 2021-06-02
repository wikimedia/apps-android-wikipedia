package org.wikipedia.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteQueryBuilder;

public class DatabaseClient<T> {
    @NonNull private final DatabaseTable<T> databaseTable;

    public DatabaseClient(@NonNull DatabaseTable<T> databaseTable) {
        this.databaseTable = databaseTable;
    }

    public void persist(T obj) {
        AppDatabase.Companion.getAppDatabase().getWritableDatabase().insert(databaseTable.getTableName(),
                SQLiteDatabase.CONFLICT_REPLACE, toContentValues(obj));
    }

    public Cursor select(@Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return AppDatabase.Companion.getAppDatabase().getReadableDatabase()
                .query(SupportSQLiteQueryBuilder.builder(databaseTable.getTableName())
                        .selection(selection, selectionArgs)
                        .orderBy(sortOrder)
                        .create());
    }

    public void deleteAll() {
        deleteWhere("", new String[] {});
    }

    public void deleteWhere(String selection, String[] selectionArgs) {
        AppDatabase.Companion.getAppDatabase().getWritableDatabase()
                .delete(databaseTable.getTableName(), selection, selectionArgs);
    }

    public void delete(@NonNull T obj, @NonNull String[] selectionArgs) {
        AppDatabase.Companion.getAppDatabase().getWritableDatabase()
                .delete(databaseTable.getTableName(),
                        getPrimaryKeySelection(obj, selectionArgs), getPrimaryKeySelectionArgs(obj));
    }

    // TODO: migrate old tables to use unique constraints and just call insertWithOnConflict.
    public void upsert(@NonNull T obj, @NonNull String[] selectionArgs) {
        int rowsUpdated = AppDatabase.Companion.getAppDatabase().getWritableDatabase()
                .update(databaseTable.getTableName(), SQLiteDatabase.CONFLICT_REPLACE,
                        toContentValues(obj), getPrimaryKeySelection(obj, selectionArgs),
                        getPrimaryKeySelectionArgs(obj));
        if (rowsUpdated == 0) {
            // TODO: synchronize with other writes. There are two operations performed.
            persist(obj);
        }
    }

    public T fromCursor(Cursor cursor) {
        return databaseTable.fromCursor(cursor);
    }

    public ContentValues toContentValues(T obj) {
        return databaseTable.toContentValues(obj);
    }

    public String getPrimaryKeySelection(@NonNull T obj, @NonNull String[] selectionArgs) {
        return databaseTable.getPrimaryKeySelection(obj, selectionArgs);
    }

    public String[] getPrimaryKeySelectionArgs(@NonNull T obj) {
        return databaseTable.getPrimaryKeySelectionArgs(obj);
    }

    protected Uri uri() {
        return databaseTable.getBaseContentURI();
    }
}
