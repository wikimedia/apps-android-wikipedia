package org.wikipedia.util

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
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
