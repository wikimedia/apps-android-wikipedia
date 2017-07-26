package org.wikipedia.offline;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import java.util.Collections;
import java.util.List;

public abstract class DownloadObserverFragment extends Fragment {

    private DownloadManagerObserver downloadObserver = new DownloadManagerObserver();
    private DownloadObserverCallback downloadObserverCallback = new DownloadObserverCallback();
    @NonNull private List<DownloadManagerItem> currentDownloads = Collections.emptyList();

    @Override
    public void onPause() {
        downloadObserver.unregister();
        super.onPause();
    }

    @Override
    public void onResume() {
        downloadObserver.register(downloadObserverCallback);
        super.onResume();
    }

    protected abstract void onPollDownloads();

    protected DownloadManagerObserver getDownloadObserver() {
        return downloadObserver;
    }

    @NonNull
    protected List<DownloadManagerItem> getCurrentDownloads() {
        return currentDownloads;
    }

    private class DownloadObserverCallback implements DownloadManagerObserver.Callback {
        @Override
        public void onDownloadStatus(@NonNull List<DownloadManagerItem> items) {
            currentDownloads = items;
            onPollDownloads();
        }
    }
}
