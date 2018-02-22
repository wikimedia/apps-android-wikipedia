package org.wikipedia.offline;

import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DownloadManagerObserver {
    private static final int POLL_INTERVAL_MS = 500;

    public interface Callback {
        void onDownloadStatus(@NonNull List<DownloadManagerItem> items);
    }

    @Nullable private DownloadManager downloadManager;
    @Nullable private Callback callback;
    @Nullable private Handler handler;
    private PollRunnable pollRunnable = new PollRunnable();

    private SparseArrayCompat<Long> downloadAmountCache = new SparseArrayCompat<>();
    private SparseArrayCompat<Long> downloadTimeMillis = new SparseArrayCompat<>();

    public void register(@NonNull Callback callback) {
        downloadManager = (DownloadManager) WikipediaApp.getInstance().getSystemService(Context.DOWNLOAD_SERVICE);
        handler = new Handler(WikipediaApp.getInstance().getMainLooper());
        this.callback = callback;
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    public void unregister() {
        downloadManager = null;
        callback = null;
        handler = null;
    }

    public boolean isDownloading(@NonNull Compilation compilation) {
        for (DownloadManagerItem item : queryCurrentDownloads()) {
            if (item.is(compilation)) {
                return true;
            }
        }
        return false;
    }

    public void remove(@NonNull Compilation compilation) {
        if (downloadManager == null) {
            return;
        }
        for (DownloadManagerItem item : queryCurrentDownloads()) {
            if (item.is(compilation)) {
                downloadManager.remove(item.id());
            }
        }
    }

    public void removeWithConfirmation(@NonNull Context context,
                                       @NonNull final Compilation compilation,
                                       @NonNull final DialogInterface.OnClickListener onRemoveClick) {
        new AlertDialog.Builder(context)
                .setMessage(R.string.compilation_remove_confirm)
                .setPositiveButton(android.R.string.yes, (dialog, i) -> {
                    remove(compilation);
                    OfflineManager.instance().remove(compilation);
                    onRemoveClick.onClick(dialog, i);
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private class PollRunnable implements Runnable {
        @Override
        public void run() {
            if (handler == null) {
                return;
            }
            List<DownloadManagerItem> items = queryCurrentDownloads();
            if (callback != null) {
                callback.onDownloadStatus(items);
            }

            handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
        }
    }

    @NonNull
    private List<DownloadManagerItem> queryCurrentDownloads() {
        List<DownloadManagerItem> items = new ArrayList<>();
        if (downloadManager == null) {
            return items;
        }
        DownloadManager.Query query = new DownloadManager.Query();
        Cursor cursor = downloadManager.query(query);
        while (cursor.moveToNext()) {
            String mimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
            if (!Compilation.MIME_TYPE.equals(mimeType)) {
                continue;
            }

            int id = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
            String uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            long bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            long bytesTotal = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

            long bytesPerSec = -1;

            if (downloadAmountCache.get(id) == null) {
                downloadAmountCache.put(id, bytesDownloaded);
                downloadTimeMillis.put(id, System.currentTimeMillis());
            }

            if (System.currentTimeMillis() > downloadTimeMillis.get(id)
                    && bytesDownloaded > downloadAmountCache.get(id)) {
                bytesPerSec = (bytesDownloaded - downloadAmountCache.get(id))
                        * TimeUnit.SECONDS.toMillis(1)
                        / (System.currentTimeMillis() - downloadTimeMillis.get(id));
                downloadAmountCache.put(id, bytesDownloaded);
                downloadTimeMillis.put(id, System.currentTimeMillis());
            }
            if (!TextUtils.isEmpty(uri)) {
                items.add(new DownloadManagerItem(id, Uri.parse(uri), status, bytesDownloaded,
                        bytesTotal, bytesPerSec));
            }
        }
        cursor.close();
        return items;
    }
}
