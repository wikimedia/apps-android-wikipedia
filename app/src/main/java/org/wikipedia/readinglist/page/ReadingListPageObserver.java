package org.wikipedia.readinglist.page;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.savedpages.SavedPageSyncService;

import java.util.ArrayList;
import java.util.List;

public class ReadingListPageObserver extends ContentObserver {
    public interface Listener {
        void onReadingListPageStatusChanged();
    }

    @NonNull private List<Listener> listeners = new ArrayList<>();

    public ReadingListPageObserver(@Nullable Handler handler) {
        super(handler);
    }

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    @Override public void onChange(boolean selfChange) {
        onChange(selfChange, null);
    }

    @Override public void onChange(boolean selfChange, Uri uri) {
        if (uri.equals(ReadingListPageContract.Disk.URI)) {
            notifyListeners();
        }
        WikipediaApp.getInstance().startService(new Intent(WikipediaApp.getInstance(), SavedPageSyncService.class));
    }

    public void register(@NonNull Context context) {
        context.getContentResolver().registerContentObserver(ReadingListPageContract.Disk.URI, false, this);
    }

    private void notifyListeners() {
        for (Listener listener : listeners) {
            listener.onReadingListPageStatusChanged();
        }
    }
}
