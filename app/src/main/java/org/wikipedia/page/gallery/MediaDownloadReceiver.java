package org.wikipedia.page.gallery;

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

import org.wikipedia.R;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.FileUtil;

public class MediaDownloadReceiver extends BroadcastReceiver {
    private static final String FILE_NAMESPACE = "File:";

    private Activity activity;
    private DownloadManager downloadManager;

    public MediaDownloadReceiver(Activity activity) {
        this.activity = activity;
        downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public void download(GalleryItem galleryItem) {
        String saveFilename = FileUtil.sanitizeFileName(trimFileNamespace(galleryItem.getName()));
        String targetDirectory;
        if (FileUtil.isVideo(galleryItem.getMimeType())) {
            targetDirectory = Environment.DIRECTORY_MOVIES;
        } else if (FileUtil.isAudio(galleryItem.getMimeType())) {
            targetDirectory = Environment.DIRECTORY_MUSIC;
        } else if (FileUtil.isImage(galleryItem.getMimeType())) {
            targetDirectory = Environment.DIRECTORY_PICTURES;
        } else {
            targetDirectory = Environment.DIRECTORY_DOWNLOADS;
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(galleryItem.getUrl()));
        request.setDestinationInExternalFilesDir(activity, targetDirectory, saveFilename);
        request.setMimeType(galleryItem.getMimeType());
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
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
            if (c.moveToFirst()) {
                int statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int pathIndex = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
                int mimeIndex = c.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE);
                if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(statusIndex)) {
                    notifyContentResolver(c.getString(pathIndex), c.getString(mimeIndex));
                    FeedbackUtil.showMessage(activity, R.string.gallery_save_success);
                }
            }
        }
    }

    private String trimFileNamespace(String filename) {
        return filename.startsWith(FILE_NAMESPACE) ? filename.substring(FILE_NAMESPACE.length()) : filename;
    }

    private void notifyContentResolver(String path, String mimeType) {
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

