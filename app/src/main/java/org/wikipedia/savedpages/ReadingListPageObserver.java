package org.wikipedia.savedpages;

import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;

public class ReadingListPageObserver extends ContentObserver {
    public ReadingListPageObserver(@Nullable Handler handler) {
        super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
        onChange(selfChange, null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        WikipediaApp.getInstance()
                .startService(new Intent(WikipediaApp.getInstance(), SavedPageSyncService.class));
    }
}
