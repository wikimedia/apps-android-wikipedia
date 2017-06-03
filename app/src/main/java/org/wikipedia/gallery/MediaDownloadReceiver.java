package org.wikipedia.gallery;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.FileUtil;

import java.io.File;

public class MediaDownloadReceiver extends BroadcastReceiver {
    private static final String FILE_NAMESPACE = "File:";

    @NonNull private Activity activity;
    @NonNull private DownloadManager downloadManager;

    public MediaDownloadReceiver(@NonNull Activity activity) {
        this.activity = activity;
        downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public void download(@NonNull FeaturedImage featuredImage) {
        String targetFileName = FileUtil.sanitizeFileName(featuredImage.title());
        String targetDirectoryType = Environment.DIRECTORY_PICTURES;
        performDownloadRequest(featuredImage.image().source(), targetDirectoryType, targetFileName, null);
    }

    public void download(@NonNull GalleryItem galleryItem) {
        String saveFilename = FileUtil.sanitizeFileName(trimFileNamespace(galleryItem.getName()));
        String targetDirectoryType;
        if (FileUtil.isVideo(galleryItem.getMimeType())) {
            targetDirectoryType = Environment.DIRECTORY_MOVIES;
        } else if (FileUtil.isAudio(galleryItem.getMimeType())) {
            targetDirectoryType = Environment.DIRECTORY_MUSIC;
        } else if (FileUtil.isImage(galleryItem.getMimeType())) {
            targetDirectoryType = Environment.DIRECTORY_PICTURES;
        } else {
            targetDirectoryType = Environment.DIRECTORY_DOWNLOADS;
        }
        performDownloadRequest(Uri.parse(galleryItem.getUrl()), targetDirectoryType, saveFilename,
                galleryItem.getMimeType());
    }

    private void performDownloadRequest(@NonNull Uri uri, @NonNull String targetDirectoryType,
                                        @NonNull String targetFileName, @Nullable String mimeType) {
        final String targetSubfolderName = WikipediaApp.getInstance().getString(R.string.app_name);
        final File categoryFolder = Environment.getExternalStoragePublicDirectory(targetDirectoryType);
        final File targetFolder = new File(categoryFolder, targetSubfolderName);
        final File targetFile = new File(targetFolder, targetFileName);

        // creates the directory if it doesn't exist else it's harmless
        targetFolder.mkdir();

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setDestinationUri(Uri.fromFile(targetFile));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        if (mimeType != null) {
            request.setMimeType(mimeType);
        }
        request.allowScanningByMediaScanner();

        downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
        FeedbackUtil.showMessage(activity, R.string.gallery_save_progress);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            Cursor c = downloadManager.query(query);
            try {
                if (c.moveToFirst()) {
                    int statusIndex = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS);
                    int pathIndex = c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI);
                    int mimeIndex = c.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE);
                    if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(statusIndex)) {
                        FeedbackUtil.showMessage(activity, R.string.gallery_save_success);
                        notifyContentResolver(Uri.parse(c.getString(pathIndex)).getPath(),
                                c.getString(mimeIndex));
                    }
                }
            } finally {
                c.close();
            }
        }
    }

    @NonNull private String trimFileNamespace(@NonNull String filename) {
        return filename.startsWith(FILE_NAMESPACE) ? filename.substring(FILE_NAMESPACE.length()) : filename;
    }

    private void notifyContentResolver(@NonNull String path, @NonNull String mimeType) {
        ContentValues values = new ContentValues();
        Uri contentUri;
        if (FileUtil.isVideo(mimeType)) {
            values.put(MediaStore.Video.Media.DATA, path);
            values.put(MediaStore.Video.Media.MIME_TYPE, mimeType);
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if (FileUtil.isAudio(mimeType)) {
            values.put(MediaStore.Audio.Media.DATA, path);
            values.put(MediaStore.Audio.Media.MIME_TYPE, mimeType);
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        } else {
            values.put(MediaStore.Images.Media.DATA, path);
            values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
        activity.getContentResolver().insert(contentUri, values);
    }
}
