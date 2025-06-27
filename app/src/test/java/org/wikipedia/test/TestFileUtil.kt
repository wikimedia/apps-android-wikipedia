package org.wikipedia.test

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

object TestFileUtil {
    private const val RAW_DIR = "src/test/res/raw/"

    @Throws(IOException::class)
    fun readRawFile(basename: String?): String {
        return String(
            Files.readAllBytes(Paths.get(RAW_DIR + basename)),
            StandardCharsets.UTF_8
        )
    }
}
