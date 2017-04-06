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
        if (callback != null && uri == ReadingListPageContract.Disk.URI) {
            callback.onChange();
        }
        WikipediaApp.getInstance().startService(new Intent(WikipediaApp.getInstance(), SavedPageSyncService.class));
    }

    public void register(@NonNull Context context) {
        context.getContentResolver().registerContentObserver(ReadingListPageContract.Disk.URI, false, this);
    }
}
