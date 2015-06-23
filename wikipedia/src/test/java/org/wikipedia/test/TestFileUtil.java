package org.wikipedia.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public final class TestFileUtil {
    private static final String MULTILINE_START_ANCHOR_REGEX = "\\A";
    private static final String RAW_DIR = "src/test/res/raw/";

    public static String readRawFile(String basename) throws FileNotFoundException {
        return readFile(RAW_DIR + basename);
    }

    public static String readFile(String filename) throws FileNotFoundException {
        return readFile(new File(filename));
    }

    public static String readFile(File file) throws FileNotFoundException {
        Scanner scanner = new Scanner(file);
        String ret = scanner.useDelimiter(MULTILINE_START_ANCHOR_REGEX).next();
        scanner.close();
        return ret;
    }

    private TestFileUtil() { }
}