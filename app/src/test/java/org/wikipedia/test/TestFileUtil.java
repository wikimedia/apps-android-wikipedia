package org.wikipedia.test;

import androidx.annotation.NonNull;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class TestFileUtil {
    private static final String RAW_DIR = "src/test/res/raw/";

    private static File getRawFile(@NonNull String rawFileName) {
        return new File(RAW_DIR + rawFileName);
    }

    public static String readRawFile(String basename) throws IOException {
        return readFile(getRawFile(basename));
    }

    private static String readFile(File file) throws IOException {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    private TestFileUtil() { }
}
