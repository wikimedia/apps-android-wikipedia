package org.wikipedia.savedpages;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import org.wikipedia.page.PageFragment;

public class SavedPageCheckCallbacks implements LoaderManager.LoaderCallbacks<Cursor>  {
    private PageFragment callingFragment;
    private Context context;

    public SavedPageCheckCallbacks(PageFragment fragment, Context context) {
        this.callingFragment = fragment;
        this.context = context;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        SavedPage dummyPage = new SavedPage(callingFragment.getTitle(), null);
        String selection = SavedPage.PERSISTENCE_HELPER.getPrimaryKeySelection(dummyPage, SavedPage.PERSISTENCE_HELPER.SELECTION_KEYS);
        String[] selectionArgs = SavedPage.PERSISTENCE_HELPER.getPrimaryKeySelectionArgs(dummyPage);
        return new CursorLoader(
                context,
                Uri.parse(SavedPage.PERSISTENCE_HELPER.getBaseContentURI().toString()),
                null,
                selection,
                selectionArgs,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        callingFragment.setPageSaved(cursor.getCount() != 0);
        callingFragment.setSavedPageCheckComplete(true);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) { }
}
