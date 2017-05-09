package org.wikipedia.dataclient.okhttp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.test.TestFileUtil;
import org.wikipedia.test.TestRunner;

import java.io.ByteArrayInputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(TestRunner.class) public class OkHttpWebViewClientTest {

    @Test public void testTransformSvgFile() throws Throwable {

        String badSVG = TestFileUtil.readRawFile("emc2_with_ex_units.svg");
        String goodSVG = TestFileUtil.readRawFile("emc2_with_em_units.svg");

        String transformedBadSVG = TestFileUtil.readStream(OkHttpWebViewClient
                .transformSvgFile(new ByteArrayInputStream(badSVG.getBytes())));

        String transformedGoodSVG = TestFileUtil.readStream(OkHttpWebViewClient
                .transformSvgFile(new ByteArrayInputStream(goodSVG.getBytes())));

        assertThat(transformedBadSVG, is(transformedGoodSVG));
    }
}
