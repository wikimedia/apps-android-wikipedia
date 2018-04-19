package org.wikipedia.page.linkpreview;

import android.text.SpannableStringBuilder;
import android.text.style.SuperscriptSpan;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.TestFileUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;

@RunWith(RobolectricTestRunner.class)
public class LinkPreviewContentsTest {
    private static final int EXPECTED_SUPS = 3;

    private static WikiSite TEST = WikiSite.forLanguageCode("test");
    private RbPageSummary rbPageSummary;

    @Before public void setUp() throws Throwable {
        String json = TestFileUtil.readRawFile("rb_page_summary_valid.json");
        rbPageSummary = GsonUnmarshaller.unmarshal(RbPageSummary.class, json);
    }

    @Test public void testExtractHasSuperscripts() throws Throwable {
        LinkPreviewContents linkPreviewContents = new LinkPreviewContents(rbPageSummary, TEST);
        SpannableStringBuilder extract = (SpannableStringBuilder) linkPreviewContents.getExtract();
        assertThat(extract.getSpans(0, extract.length(), SuperscriptSpan.class),
                arrayWithSize(EXPECTED_SUPS)); // the 3 <sup> tags in the formula are represented correctly
    }
}
