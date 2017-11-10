package org.wikipedia.readinglist.sync;

import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ReadingListSyncService extends Service {
    @NonNull private static final Object SYNC_ADAPTER_LOCK = new Object();
    @Nullable private static AbstractThreadedSyncAdapter SYNC_ADAPTER;

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (SYNC_ADAPTER_LOCK) {
            if (SYNC_ADAPTER == null) {
                SYNC_ADAPTER = new ReadingListSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return SYNC_ADAPTER == null ? null : SYNC_ADAPTER.getSyncAdapterBinder();
    }
}
