package org.wikipedia.util;

import android.graphics.Bitmap;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public final class FileUtil {
    public static final int JPEG_QUALITY = 85;
    private static final int KB16 = 16 * 1024;

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

    /**
     * Write a JSON object to a file
     * @param file file to be written
     * @param jsonObject content of file
     * @throws IOException when writing failed
     */
    public static void writeToFile(File file, JSONObject jsonObject) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file));
        try {
            writer.write(jsonObject.toString());
        } finally {
            writer.close();
        }
    }

    public static ByteArrayOutputStream compressBmpToJpg(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, bytes);
        return bytes;
    }

    /**
     * Deletes a file or directory, with optional recursion.
     * @param path File or directory to delete.
     * @param recursive Whether to delete all subdirectories and files.
     */
    public static void delete(File path, boolean recursive) {
        if (recursive && path.isDirectory()) {
            String[] children = path.list();
            for (String child : children) {
                delete(new File(path, child), recursive);
            }
        }
        path.delete();
    }

    public static void writeFile(InputStream inputStream, File file) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(file);
        try {
            copyStreams(inputStream, outputStream);
        } finally {
            outputStream.close();
        }
    }

    /**
     * Utility method to copy a stream into another stream.
     *
     * Uses a 16KB buffer.
     *
     * @param in Stream to copy from.
     * @param out Stream to copy to.
     * @throws IOException
     */
    public static void copyStreams(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[KB16]; // 16kb buffer
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
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

    /**
     * Write the contents of a String to a stream.
     * @param outputStream Stream to which the contents will be written.
     * @param contents String with the contents to be written.
     * @throws IOException
     */
    public static void writeToStream(OutputStream outputStream, String contents) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        try {
            writer.write(contents);
        } finally {
            writer.close();
        }
    }

    /**
     * Reads the contents of this page from storage.
     * @return Page object with the contents of the page.
     * @throws IOException
     * @throws JSONException
     */
    public static JSONObject readJSONFile(File f) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        try {
            StringBuilder stringBuilder = new StringBuilder();
            String readStr;
            while ((readStr = reader.readLine()) != null) {
                stringBuilder.append(readStr);
            }
            return new JSONObject(stringBuilder.toString());
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

    /**
     * Gets the base directory for all saved pages.
     * (will be something like /data/data/org.wikimedia/files/savedpages)
     * @return Base directory for saved pages, inside the app's private storage space.
     */
    public static String savedPageBaseDir() {
        return WikipediaApp.getInstance().getFilesDir() + "/savedpages";
    }

    /**
     * Gets the base directory for this page's saved files.
     * The name of the directory is the MD5 sum of the page title (to account for special
     * or unicode characters in the title).
     * @return Base directory for the saved files for this page, inside the
     * overall base directory for saved pages.
     */
    public static String getSavedPageDirFor(PageTitle title) {
        String dir = savedPageBaseDir() + "/" + title.getIdentifier();
        (new File(dir)).mkdirs();
        return dir;
    }

    private FileUtil() {

    }
}
