package org.wikipedia.database;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;

public abstract class CursorAdapterLoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {
    @NonNull private final Context context;
    @NonNull private final CursorAdapter adapter;

    public CursorAdapterLoaderCallback(@NonNull Context context, @NonNull CursorAdapter adapter) {
        this.context = context.getApplicationContext();
        this.adapter = adapter;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        adapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.changeCursor(null);
    }

    @NonNull protected Context context() {
        return context;
    }
}
