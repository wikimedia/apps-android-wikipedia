package org.wikipedia.savedpages;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.database.contract.ReadingListPageContract;

public class ReadingListPageObserver extends ContentObserver {
    public interface Callback {
        void onChange();
    }

    @Nullable private Callback callback;

    public ReadingListPageObserver(@Nullable Handler handler) {
        super(handler);
    }

    public void setCallback(@Nullable Callback cb) {
        callback = cb;
    }

    @Override public void onChange(boolean selfChange) {
        onChange(selfChange, null);
    }

    @Override public void onChange(boolean selfChange, Uri uri) {
        // The content observer is notified of changes to the registered URI's parent (here,
        // content://org.wikipedia.dev/readinglist/page) as well as the specific URI
        // we're interested in. We can double-check the URI here to prevent multiple calls to
        // notifyDataSetChanged for the same underlying change, which could otherwise produce an
        // unpleasant flickering effect on the changed feed card, especially if it has an image.
        if (uri != ReadingListPageContract.Disk.URI) {
            return;
        }
        if (callback != null) {
            callback.onChange();
        }
        WikipediaApp.getInstance().startService(new Intent(WikipediaApp.getInstance(), SavedPageSyncService.class));
    }

    public void register(@NonNull Context context) {
        context.getContentResolver().registerContentObserver(ReadingListPageContract.Disk.URI, false, this);
    }
}
