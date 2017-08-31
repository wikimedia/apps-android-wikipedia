package org.wikipedia.offline;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DownloadManagerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
            if (intent.getExtras() == null || !intent.hasExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS)) {
                return;
            }
            long[] ids = intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            for (long id : ids) {
                if (Compilation.MIME_TYPE.equals(downloadManager.getMimeTypeForDownloadedFile(id))) {
                    context.startActivity(LocalCompilationsActivity.
                            newIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    break;
                }
            }
        }
    }
}
