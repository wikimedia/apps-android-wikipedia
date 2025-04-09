package org.wikipedia.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtil {
    private const val JPEG_QUALITY = 85

    fun writeToFile(bytes: ByteArrayOutputStream, destinationFile: File): File {
        destinationFile.outputStream().use {
            it.write(bytes.toByteArray())
            it.flush()
        }
        return destinationFile
    }

    fun createFileInDownloadsFolder(context: Context, filename: String, mimeType: String, data: String?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentResolver = context.contentResolver
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                contentResolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)?.let { uri ->
                    contentResolver.openOutputStream(uri)?.use { it.write(data?.toByteArray()) }
                }
            } else {
                val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val exportFile = File(downloadsFolder, filename)
                exportFile.delete() // To overwrite when file exists
                FileOutputStream(exportFile, true).bufferedWriter().use { it.write(data) }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun compressBmpToJpg(bitmap: Bitmap): ByteArrayOutputStream {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, bytes)
        return bytes
    }

    fun readFile(inputStream: InputStream): String {
        return inputStream.reader().readLines().joinToString(separator = "\n")
    }

    fun sanitizeFileName(fileName: String): String {
        return fileName.replace("[:\\\\/*\"?|<>']".toRegex(), "_")
    }

    fun isVideo(mimeType: String): Boolean {
        return mimeType.contains("ogg") || mimeType.contains("video")
    }

    fun isAudio(mimeType: String): Boolean {
        return mimeType.contains("audio")
    }

    fun isImage(mimeType: String): Boolean {
        return mimeType.contains("image")
    }
}
