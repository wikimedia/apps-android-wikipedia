package org.wikipedia.test;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Scanner;

public final class TestFileUtil {
    private static final String MULTILINE_START_ANCHOR_REGEX = "\\A";
    private static final String RAW_DIR = "src/test/res/raw/";

    public static File getRawFile(@NonNull String rawFileName) {
        return new File(RAW_DIR + rawFileName);
    }

    public static String readRawFile(String basename) throws FileNotFoundException {
        return readFile(getRawFile(basename));
    }

    private static String readFile(File file) throws FileNotFoundException {
        Scanner scanner = new Scanner(file);
        String ret = scanner.useDelimiter(MULTILINE_START_ANCHOR_REGEX).next();
        scanner.close();
        return ret;
    }

    public static String readStream(InputStream stream) throws FileNotFoundException {
        Scanner scanner = new Scanner(stream);
        String ret = scanner.useDelimiter(MULTILINE_START_ANCHOR_REGEX).next();
        scanner.close();
        return ret;
    }

    private TestFileUtil() { }
}
