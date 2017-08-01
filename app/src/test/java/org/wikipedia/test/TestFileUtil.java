package org.wikipedia.test;

import android.annotation.TargetApi;
import android.support.annotation.NonNull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public final class TestFileUtil {
    private static final String RAW_DIR = "src/test/res/raw/";

    public static File getRawFile(@NonNull String rawFileName) {
        return new File(RAW_DIR + rawFileName);
    }

    public static String readRawFile(String basename) throws IOException {
        return readFile(getRawFile(basename));
    }

    @TargetApi(19)
    private static String readFile(File file) throws IOException {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    @TargetApi(19)
    public static String readStream(InputStream stream) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(stream, writer, StandardCharsets.UTF_8);
        return writer.toString();
    }

    private TestFileUtil() { }
}
