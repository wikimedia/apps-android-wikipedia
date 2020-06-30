package org.wikipedia.gallery;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.FileUtil;

import java.io.File;

public class MediaDownloadReceiver extends BroadcastReceiver {
    private static final String FILE_NAMESPACE = "File:";

    public interface Callback {
        void onSuccess();
    }

    @Nullable private Callback callback;

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void download(@NonNull Context context, @NonNull FeaturedImage featuredImage) {
        String filename = FileUtil.sanitizeFileName(featuredImage.title());
        String targetDirectory = Environment.DIRECTORY_PICTURES;
        performDownloadRequest(context, Uri.parse(featuredImage.getOriginal().getSource()), targetDirectory, filename, null);
    }

    public void download(@NonNull Context context, @NonNull PageTitle imageTitle, @NonNull ImageInfo mediaInfo) {
        String saveFilename = FileUtil.sanitizeFileName(trimFileNamespace(imageTitle.getDisplayText()));
        String fileUrl = mediaInfo.getOriginalUrl();
        String targetDirectoryType;
        if (FileUtil.isVideo(mediaInfo.getMimeType()) && mediaInfo.getBestDerivative() != null) {
            targetDirectoryType = Environment.DIRECTORY_MOVIES;
            fileUrl = mediaInfo.getBestDerivative().getSrc();
        } else if (FileUtil.isAudio(mediaInfo.getMimeType())) {
            targetDirectoryType = Environment.DIRECTORY_MUSIC;
        } else if (FileUtil.isImage(mediaInfo.getMimeType())) {
            targetDirectoryType = Environment.DIRECTORY_PICTURES;
        } else {
            targetDirectoryType = Environment.DIRECTORY_DOWNLOADS;
        }
        performDownloadRequest(context, Uri.parse(fileUrl), targetDirectoryType, saveFilename, mediaInfo.getMimeType());
    }

    private void performDownloadRequest(@NonNull Context context, @NonNull Uri uri,
                                        @NonNull String targetDirectoryType,
                                        @NonNull String targetFileName, @Nullable String mimeType) {
        final String targetSubfolderName = WikipediaApp.getInstance().getString(R.string.app_name);
        final File categoryFolder = Environment.getExternalStoragePublicDirectory(targetDirectoryType);
        final File targetFolder = new File(categoryFolder, targetSubfolderName);
        final File targetFile = new File(targetFolder, targetFileName);

        // creates the directory if it doesn't exist else it's harmless
        targetFolder.mkdir();

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setDestinationUri(Uri.fromFile(targetFile));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        if (mimeType != null) {
            request.setMimeType(mimeType);
        }
        request.allowScanningByMediaScanner();
        downloadManager.enqueue(request);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            try (Cursor c = downloadManager.query(query)) {
                if (c.moveToFirst()) {
                    int statusIndex = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS);
                    int pathIndex = c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI);
                    int mimeIndex = c.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE);
                    if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(statusIndex)) {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                        notifyContentResolver(context, Uri.parse(c.getString(pathIndex)).getPath(),
                                c.getString(mimeIndex));
                    }
                }
            }
        }
    }

    @NonNull private static String trimFileNamespace(@NonNull String filename) {
        return filename.startsWith(FILE_NAMESPACE) ? filename.substring(FILE_NAMESPACE.length()) : filename;
    }

    // TODO: Research whether this whole call is necessary anymore.
    private void notifyContentResolver(@NonNull Context context, @Nullable String path, @NonNull String mimeType) {
        ContentValues values = new ContentValues();
        Uri contentUri = null;
        if (FileUtil.isVideo(mimeType)) {
            values.put(MediaStore.Video.Media.DATA, path);
            values.put(MediaStore.Video.Media.MIME_TYPE, mimeType);
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if (FileUtil.isAudio(mimeType)) {
            values.put(MediaStore.Audio.Media.DATA, path);
            values.put(MediaStore.Audio.Media.MIME_TYPE, mimeType);
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        } else if (!TextUtils.isEmpty(path)) {
            values.put(MediaStore.Images.Media.DATA, path);
            values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
        if (contentUri != null) {
            try {
                context.getContentResolver().insert(contentUri, values);
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
