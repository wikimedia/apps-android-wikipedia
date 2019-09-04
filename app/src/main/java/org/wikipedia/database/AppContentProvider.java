package org.wikipedia.database;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.WikipediaApp;
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

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(endpoint.tables());

        SQLiteDatabase db = readableDatabase();
        final String groupBy = null;
        final String having = null;

        if (LOG) {
            L.d("selectionArgs=" + Arrays.toString(selectionArgs));
            String sql = builder.buildQuery(projection, "(" + selection + ")", groupBy, having,
                    sortOrder, null);
            L.d("sql=" + sql);
        }

        Cursor cursor = builder.query(db, projection == null ? endpoint.projection() : projection,
                selection, selectionArgs, groupBy, having, sortOrder);

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

        SQLiteDatabase db = writableDatabase();
        final String nullColumnHack = null;
        db.replaceOrThrow(endpoint.tables(), nullColumnHack, values);

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

        SQLiteDatabase db = writableDatabase();
        int rows = db.delete(endpoint.tables(), selection, selectionArgs);

        notifyChange(uri);
        return rows;
    }

    @Override public int update(@NonNull Uri uri, @Nullable ContentValues values,
                                @Nullable String selection, @Nullable String[] selectionArgs) {
        AppContentProviderEndpoint endpoint = AppContentProviderEndpoint.of(uri);

        SQLiteDatabase db = writableDatabase();
        int rows = db.update(endpoint.tables(), values, selection, selectionArgs);

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

    private SQLiteDatabase readableDatabase() {
        return WikipediaApp.getInstance().getDatabase().getReadableDatabase();
    }

    private SQLiteDatabase writableDatabase() {
        return WikipediaApp.getInstance().getDatabase().getWritableDatabase();
    }
}
