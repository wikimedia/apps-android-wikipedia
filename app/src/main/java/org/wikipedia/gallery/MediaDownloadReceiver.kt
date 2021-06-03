package org.wikipedia.gallery

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.contentValuesOf
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.feed.image.FeaturedImage
import org.wikipedia.page.PageTitle
import org.wikipedia.util.FileUtil.isAudio
import org.wikipedia.util.FileUtil.isImage
import org.wikipedia.util.FileUtil.isVideo
import org.wikipedia.util.FileUtil.sanitizeFileName
import java.io.File

class MediaDownloadReceiver : BroadcastReceiver() {
    interface Callback {
        fun onSuccess()
    }

    var callback: Callback? = null

    fun download(context: Context, featuredImage: FeaturedImage) {
        val filename = sanitizeFileName(featuredImage.title())
        val targetDirectory = Environment.DIRECTORY_PICTURES
        performDownloadRequest(context, featuredImage.original.source.toUri(), targetDirectory, filename, null)
    }

    fun download(context: Context, imageTitle: PageTitle, mediaInfo: ImageInfo) {
        val saveFilename = sanitizeFileName(trimFileNamespace(imageTitle.displayText))
        var fileUrl = mediaInfo.originalUrl
        val targetDirectoryType: String
        if (isVideo(mediaInfo.mimeType) && mediaInfo.bestDerivative != null) {
            targetDirectoryType = Environment.DIRECTORY_MOVIES
            fileUrl = mediaInfo.bestDerivative!!.src
        } else if (isAudio(mediaInfo.mimeType)) {
            targetDirectoryType = Environment.DIRECTORY_MUSIC
        } else if (isImage(mediaInfo.mimeType)) {
            targetDirectoryType = Environment.DIRECTORY_PICTURES
        } else {
            targetDirectoryType = Environment.DIRECTORY_DOWNLOADS
        }
        performDownloadRequest(context, fileUrl.toUri(), targetDirectoryType, saveFilename, mediaInfo.mimeType)
    }

    private fun performDownloadRequest(context: Context, uri: Uri, targetDirectoryType: String,
                                       targetFileName: String, mimeType: String?) {
        val targetSubfolderName = WikipediaApp.getInstance().getString(R.string.app_name)
        val categoryFolder = Environment.getExternalStoragePublicDirectory(targetDirectoryType)
        val targetFolder = File(categoryFolder, targetSubfolderName)
        val targetFile = File(targetFolder, targetFileName)

        // creates the directory if it doesn't exist else it's harmless
        targetFolder.mkdir()
        val downloadManager = context.getSystemService<DownloadManager>()!!
        val request = DownloadManager.Request(uri)
        request.setDestinationUri(targetFile.toUri())
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        if (mimeType != null) {
            request.setMimeType(mimeType)
        }
        request.allowScanningByMediaScanner()
        downloadManager.enqueue(request)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)
            val query = DownloadManager.Query()
            query.setFilterById(downloadId)
            val downloadManager = context.getSystemService<DownloadManager>()!!
            downloadManager.query(query).use { c ->
                if (c.moveToFirst()) {
                    val statusIndex = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                    val pathIndex = c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                    val mimeIndex = c.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE)
                    if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(statusIndex)) {
                        callback?.onSuccess()
                        notifyContentResolver(context, c.getString(pathIndex).toUri().path, c.getString(mimeIndex))
                    }
                }
            }
        }
    }

    // TODO: Research whether this whole call is necessary anymore.
    private fun notifyContentResolver(context: Context, path: String?, mimeType: String) {
        val values: ContentValues
        var contentUri: Uri? = null
        if (isVideo(mimeType)) {
            values = contentValuesOf(MediaStore.Video.Media.DATA to path, MediaStore.Video.Media.MIME_TYPE to mimeType)
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else if (isAudio(mimeType)) {
            values = contentValuesOf(MediaStore.Audio.Media.DATA to path, MediaStore.Audio.Media.MIME_TYPE to mimeType)
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        } else if (!path.isNullOrEmpty()) {
            values = contentValuesOf(MediaStore.Images.Media.DATA to path, MediaStore.Images.Media.MIME_TYPE to mimeType)
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            values = ContentValues()
        }
        if (contentUri != null) {
            try {
                context.contentResolver.insert(contentUri, values)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    companion object {
        private const val FILE_NAMESPACE = "File:"

        private fun trimFileNamespace(filename: String): String {
            return if (filename.startsWith(FILE_NAMESPACE)) filename.substring(FILE_NAMESPACE.length) else filename
        }
    }
}
