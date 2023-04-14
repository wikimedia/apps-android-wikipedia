package org.wikipedia.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class TestFileUtil {
    private static final String RAW_DIR = "src/test/res/raw/";

    public static String readRawFile(String basename) throws IOException {
        // TODO: Use Files.readString() once it is available in the Android SDK.
        return new String(Files.readAllBytes(Paths.get(RAW_DIR + basename)),
                StandardCharsets.UTF_8);
    }

    private TestFileUtil() { }
}
