package org.wikipedia.savedpages;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.wikipedia.Constants;

public class SavedPageSyncBroadcastReceiver{


    public static class SyncCancelReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getBooleanExtra(Constants.INTENT_EXTRA_NOTIFICATION_SYNC_CANCEL, false)) {
                // cancel sync service
                SavedPageSyncNotification.getInstance().setCancelSyncDownload();
            }
        }
    }

    public static class SyncPauseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getBooleanExtra(Constants.INTENT_EXTRA_NOTIFICATION_SYNC_PAUSE, false)) {
                // pause or resume sync service
                SavedPageSyncNotification.getInstance().setPauseSyncDownload();
            }
        }
    }
}
