package org.wikipedia.database;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

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
