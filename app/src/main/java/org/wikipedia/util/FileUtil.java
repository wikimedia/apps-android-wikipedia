package org.wikipedia.util;

import android.graphics.Bitmap;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class FileUtil {
    public static final int JPEG_QUALITY = 85;

    public static File writeToFile(ByteArrayOutputStream bytes, File destinationFile) throws IOException {
        FileOutputStream fo = new FileOutputStream(destinationFile);
        try {
            fo.write(bytes.toByteArray());
        } finally {
            fo.flush();
            fo.close();
        }
        return destinationFile;
    }

    public static ByteArrayOutputStream compressBmpToJpg(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, bytes);
        return bytes;
    }

    /**
     * Reads the contents of a file, preserving line breaks.
     * @return contents of the given file as a String.
     * @throws IOException
     */
    public static String readFile(final InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            StringBuilder stringBuilder = new StringBuilder();
            String readStr;
            while ((readStr = reader.readLine()) != null) {
                stringBuilder.append(readStr).append('\n');
            }
            return stringBuilder.toString();
        } finally {
            reader.close();
        }
    }

    public static void clearDirectory(File dir) {
        if (dir.isDirectory()) {
            for (String file : dir.list()) {
                new File(dir, file).delete();
            }
        }
    }

    public static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[:\\\\/*\"?|<>']", "_");
    }

    public static boolean isVideo(String mimeType) {
        return mimeType.contains("ogg") || mimeType.contains("video");
    }

    public static boolean isAudio(String mimeType) {
        return mimeType.contains("audio");
    }

    public static boolean isImage(String mimeType) {
        return mimeType.contains("image");
    }

    private FileUtil() { }
}
