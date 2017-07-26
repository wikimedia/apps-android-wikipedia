package org.wikipedia.offline;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.File;

public class DownloadManagerItem {
    private int id;
    @NonNull private Uri uri;
    private int status;
    private long bytesDownloaded;
    private long bytesTotal;
    private long bytesPerSec;

    public DownloadManagerItem(int id, @NonNull Uri uri, int status, long bytesDownloaded,
                               long bytesTotal, long bytesPerSec) {
        this.id = id;
        this.uri = uri;
        this.status = status;
        this.bytesDownloaded = bytesDownloaded;
        this.bytesTotal = bytesTotal;
        this.bytesPerSec = bytesPerSec;
    }

    public int id() {
        return id;
    }

    @NonNull
    public Uri uri() {
        return uri;
    }

    public int status() {
        return status;
    }

    public long bytesDownloaded() {
        return bytesDownloaded;
    }

    public long bytesTotal() {
        return bytesTotal;
    }

    public long bytesPerSec() {
        return bytesPerSec;
    }

    public boolean is(@NonNull Compilation compilation) {
        if (!TextUtils.isEmpty(compilation.path())) {
            return uri.getLastPathSegment().equals(new File(compilation.path()).getName());
        } else if (compilation.uri() != null) {
            return uri.getLastPathSegment().equals(compilation.uri().getLastPathSegment());
        }
        return false;
    }
}
