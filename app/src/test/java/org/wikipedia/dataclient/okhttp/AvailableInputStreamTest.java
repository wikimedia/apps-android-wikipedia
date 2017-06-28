package org.wikipedia.dataclient.okhttp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.test.TestFileUtil;
import org.wikipedia.test.TestRunner;

import java.io.ByteArrayInputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(TestRunner.class) public class AvailableInputStreamTest {

    @SuppressWarnings("checkstyle:magicnumber")
    @Test public void testAvailableInputStream() throws Throwable {

        byte[] contentBytes = TestFileUtil.readRawFile("page_lead_rb.json").getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(contentBytes);

        AvailableInputStream availableStream
                = new AvailableInputStream(inputStream, contentBytes.length);

        assertThat(availableStream.available(), is(contentBytes.length));

        byte[] tempBytes = new byte[10];
        availableStream.read();
        availableStream.read(tempBytes);
        availableStream.read(tempBytes, 0, 10);
        availableStream.skip(10);

        assertThat(availableStream.available(), is(contentBytes.length - 31));
    }
}
