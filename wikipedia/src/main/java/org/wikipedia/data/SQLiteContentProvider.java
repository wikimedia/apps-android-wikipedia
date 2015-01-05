package org.wikipedia.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import org.wikipedia.BuildConfig;

public abstract class SQLiteContentProvider<T> extends ContentProvider {
    private final PersistenceHelper<T> persistenceHelper;

    protected SQLiteContentProvider(PersistenceHelper<T> persistenceHelper) {
        this.persistenceHelper = persistenceHelper;
    }

    protected abstract DBOpenHelper getDbOpenHelper();

    private final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    protected UriMatcher getUriMatcher() {
        return uriMatcher;
    }

    private static final int MATCH_ALL = 1;

    @Override
    public boolean onCreate() {
        uriMatcher.addURI(getAuthority(), getTableName(), MATCH_ALL);
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(getTableName());

        int uriType = uriMatcher.match(uri);

        SQLiteDatabase db = getDbOpenHelper().getReadableDatabase();
        Cursor cursor;

        switch (uriType) {
            case MATCH_ALL:
                cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Uri " + uri + " does not match any matcher!");
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = getDbOpenHelper().getWritableDatabase();
        switch (uriType) {
            case MATCH_ALL:
                sqlDB.insert(getTableName(), null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int rows = 0;
        int uriType = uriMatcher.match(uri);

        SQLiteDatabase db = getDbOpenHelper().getReadableDatabase();

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
        getContext().getContentResolver().notifyChange(uri, null);
        return rows;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = getDbOpenHelper().getWritableDatabase();
        int modifiedRows;
        switch (uriType) {
            case MATCH_ALL:
                modifiedRows = sqlDB.update(getTableName(), values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return modifiedRows;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = getDbOpenHelper().getWritableDatabase();
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
        getContext().getContentResolver().notifyChange(uri, null);
        return values.length;
    }

    protected String getTableName() {
        return persistenceHelper.getTableName();
    }

    protected String getAuthority() {
        return getAuthorityForTable(getTableName());
    }

    public static String getAuthorityForTable(String table) {
        return BuildConfig.APPLICATION_ID + "." + table;
    }
}
