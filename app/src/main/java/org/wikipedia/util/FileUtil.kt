package org.wikipedia.util

import android.graphics.Bitmap
import java.io.*

object FileUtil {
    private const val JPEG_QUALITY = 85

    @JvmStatic
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

    @JvmStatic
    fun compressBmpToJpg(bitmap: Bitmap): ByteArrayOutputStream {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, bytes)
        return bytes
    }

    @JvmStatic
    fun readFile(inputStream: InputStream): String {
        return inputStream.reader().readLines().joinToString(separator = "\n")
    }

    @JvmStatic
    fun deleteRecursively(f: File) {
        if (f.isDirectory) {
            f.listFiles()?.forEach {
                deleteRecursively(it)
            }
        }
        f.delete()
    }

    @JvmStatic
    fun sanitizeFileName(fileName: String): String {
        return fileName.replace("[:\\\\/*\"?|<>']".toRegex(), "_")
    }

    @JvmStatic
    fun isVideo(mimeType: String): Boolean {
        return mimeType.contains("ogg") || mimeType.contains("video")
    }

    @JvmStatic
    fun isAudio(mimeType: String): Boolean {
        return mimeType.contains("audio")
    }

    @JvmStatic
    fun isImage(mimeType: String): Boolean {
        return mimeType.contains("image")
    }
}
