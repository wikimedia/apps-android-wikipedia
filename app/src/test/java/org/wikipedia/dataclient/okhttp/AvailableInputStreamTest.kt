package org.wikipedia.dataclient.okhttp

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.wikipedia.test.TestFileUtil
import java.io.ByteArrayInputStream

class AvailableInputStreamTest {
    @Test
    @Throws(Throwable::class)
    fun testAvailableInputStream() {
        val contentBytes = TestFileUtil.readRawFile("page_lead_rb.json").toByteArray()
        val inputStream = ByteArrayInputStream(contentBytes)

        val availableStream = AvailableInputStream(inputStream, contentBytes.size.toLong())

        MatcherAssert.assertThat(availableStream.available(), Matchers.`is`(contentBytes.size))

        val tempBytes = ByteArray(10)
        availableStream.read()
        availableStream.read(tempBytes)
        availableStream.read(tempBytes, 0, 10)
        availableStream.skip(10)

        MatcherAssert.assertThat(availableStream.available(), Matchers.`is`(contentBytes.size - 31))
    }
}
