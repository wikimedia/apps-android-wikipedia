package org.wikipedia.util;

import android.graphics.Bitmap;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class FileUtil {
    private static final String WIKIPEDIA_IMAGES_FOLDER = "Wikipedia/Media/Wikipedia Images";
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

    public static File getWikipediaImagesDirectory() {
        File wikipediaImagesDirectory = new File(Environment.getExternalStorageDirectory()
                + File.separator
                + WIKIPEDIA_IMAGES_FOLDER);
        if (!wikipediaImagesDirectory.exists()) {
            wikipediaImagesDirectory.mkdirs();
        }
        return wikipediaImagesDirectory;
    }


    public static ByteArrayOutputStream compressBmpToJpg(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, bytes);
        return bytes;
    }

    private FileUtil() {

    }
}
