package org.wikipedia.database;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.BuildConfig;
import org.wikipedia.WikipediaApp;

public abstract class SQLiteContentProvider extends ContentProvider {
    private final DatabaseTable<?> databaseTable;

    protected SQLiteContentProvider(DatabaseTable<?> databaseTable) {
        this.databaseTable = databaseTable;
    }

    protected Database getDatabase() {
        return WikipediaApp.getInstance().getDatabase();
    }

    private final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    protected UriMatcher getUriMatcher() {
        return uriMatcher;
    }

    private static final int MATCH_ALL = 1;

    @Override
    public boolean onCreate() {
        uriMatcher.addURI(getAuthority(), getTableName(), MATCH_ALL);
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(getTableName());

        int uriType = uriMatcher.match(uri);

        SQLiteDatabase db = getDatabase().getReadableDatabase();
        Cursor cursor;

        switch (uriType) {
            case MATCH_ALL:
                cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Uri " + uri + " does not match any matcher!");
        }

        cursor.setNotificationUri(getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = getDatabase().getWritableDatabase();
        switch (uriType) {
            case MATCH_ALL:
                sqlDB.insert(getTableName(), null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        notifyChange(uri, null);
        return uri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int rows;
        int uriType = uriMatcher.match(uri);

        SQLiteDatabase db = getDatabase().getReadableDatabase();

        switch(uriType) {
            case MATCH_ALL:
                rows = db.delete(getTableName(),
                        selection,
                        selectionArgs
                );
                break;
            default:
                throw new IllegalArgumentException("Unknown URI" + uri);
        }
        notifyChange(uri, null);
        return rows;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = getDatabase().getWritableDatabase();
        int modifiedRows;
        switch (uriType) {
            case MATCH_ALL:
                modifiedRows = sqlDB.update(getTableName(), values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        notifyChange(uri, null);
        return modifiedRows;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = getDatabase().getWritableDatabase();
        sqlDB.beginTransaction();
        switch (uriType) {
            case MATCH_ALL:
                for (ContentValues value: values) {
                    sqlDB.insert(getTableName(), null, value);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        sqlDB.setTransactionSuccessful();
        sqlDB.endTransaction();
        notifyChange(uri, null);
        return values.length;
    }

    protected String getTableName() {
        return databaseTable.getTableName();
    }

    protected String getAuthority() {
        return getAuthorityForTable(getTableName());
    }

    public static String getAuthorityForTable(String table) {
        return BuildConfig.APPLICATION_ID + "." + table;
    }

    protected void notifyChange(@NonNull Uri uri, @Nullable ContentObserver observer) {
        if (getContentResolver() != null) {
            getContentResolver().notifyChange(uri, observer);
        }
    }

    @Nullable
    protected ContentResolver getContentResolver() {
        return getContext() == null ? null : getContext().getContentResolver();
    }
}