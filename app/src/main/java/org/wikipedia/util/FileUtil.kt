package org.wikipedia.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtil {
    private const val JPEG_QUALITY = 85

    fun writeToFile(bytes: ByteArrayOutputStream, destinationFile: File): File {
        val fo = destinationFile.outputStream()
        try {
            fo.write(bytes.toByteArray())
        } finally {
            fo.flush()
            fo.close()
        }
        return destinationFile
    }

    fun createFileInDownloadsFolder(context: Context, filename: String, stringBuilder: StringBuilder) {
        val appExportsFolderName = WikipediaApp.instance.getString(R.string.app_name)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentResolver = context.contentResolver
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Download${File.separator}$appExportsFolderName")
                contentResolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)?.let { uri ->
                    contentResolver.openOutputStream(uri)?.use { it.write(stringBuilder.toString().toByteArray()) }
                }
            } else {
                val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val exportsFolder = File(downloadsFolder, appExportsFolderName)
                val csvFile = File(exportsFolder, filename)
                exportsFolder.mkdir()
                csvFile.delete() //To overwrite when file exists
                FileOutputStream(csvFile, true).bufferedWriter().use { writer ->
                    writer.write(stringBuilder.toString())
                }
            }
        } catch (e: Exception) {
            //ignore
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

    fun deleteRecursively(f: File) {
        if (f.isDirectory) {
            f.listFiles()?.forEach {
                deleteRecursively(it)
            }
        }
        f.delete()
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
