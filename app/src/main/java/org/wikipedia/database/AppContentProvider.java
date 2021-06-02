package org.wikipedia.database;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteQueryBuilder;

import org.wikipedia.database.contract.AppContentProviderContract;
import org.wikipedia.util.log.L;

import java.util.Arrays;

public class AppContentProvider extends ContentProvider {
    private static final boolean LOG = false;

    @Override public boolean onCreate() {
        @SuppressWarnings("UnnecessaryLocalVariable") final boolean loaded = true;
        return loaded;
    }

    @Nullable @Override public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                                            @Nullable String selection,
                                            @Nullable String[] selectionArgs,
                                            @Nullable String sortOrder) {
        AppContentProviderEndpoint endpoint = AppContentProviderEndpoint.of(uri);
        SupportSQLiteDatabase db = readableDatabase();
        final String groupBy = null;
        final String having = null;

        Cursor cursor = db.query(SupportSQLiteQueryBuilder.builder(endpoint.tables())
                .columns(projection)
                .selection(selection, selectionArgs)
                .groupBy(groupBy)
                .having(having)
                .orderBy(sortOrder)
                .create());

        if (cursor != null) {
            if (LOG) {
                L.d("count=" + cursor.getCount() + " columnNames=" + Arrays.toString(cursor.getColumnNames()));
            }
            cursor.setNotificationUri(getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable @Override public String getType(@NonNull Uri uri) {
        AppContentProviderEndpoint endpoint = AppContentProviderEndpoint.of(uri);
        return endpoint.type();
    }

    @Nullable @Override public Uri insert(@NonNull Uri uri, ContentValues values) {
        AppContentProviderEndpoint endpoint = AppContentProviderEndpoint.of(uri);

        SupportSQLiteDatabase db = writableDatabase();
        db.insert(endpoint.tables(), SQLiteDatabase.CONFLICT_REPLACE, values);

        notifyChange(uri);

        Uri itemUri = endpoint.itemUri(values);
        if (itemUri != null) {
            notifyChange(itemUri);
        }
        return itemUri;
    }

    @Override public int delete(@NonNull Uri uri, @Nullable String selection,
                                @Nullable String[] selectionArgs) {
        AppContentProviderEndpoint endpoint = AppContentProviderEndpoint.of(uri);

        SupportSQLiteDatabase db = writableDatabase();
        int rows = db.delete(endpoint.tables(), selection, selectionArgs);

        notifyChange(uri);
        return rows;
    }

    @Override public int update(@NonNull Uri uri, @Nullable ContentValues values,
                                @Nullable String selection, @Nullable String[] selectionArgs) {
        AppContentProviderEndpoint endpoint = AppContentProviderEndpoint.of(uri);

        SupportSQLiteDatabase db = writableDatabase();
        int rows = db.update(endpoint.tables(), SQLiteDatabase.CONFLICT_REPLACE, values, selection, selectionArgs);

        notifyChange(uri);
        return rows;
    }

    private void notifyChange(@NonNull Uri uri) {
        boolean notify = uri.getBooleanQueryParameter(AppContentProviderContract.NOTIFY, true);
        if (getContentResolver() == null || !notify) {
            return;
        }
        getContentResolver().notifyChange(uri, null);
    }

    @Nullable private ContentResolver getContentResolver() {
        return getContext() == null ? null : getContext().getContentResolver();
    }

    private SupportSQLiteDatabase readableDatabase() {
        return AppDatabase.Companion.getAppDatabase().getReadableDatabase();
    }

    private SupportSQLiteDatabase writableDatabase() {
        return AppDatabase.Companion.getAppDatabase().getWritableDatabase();
    }
}
