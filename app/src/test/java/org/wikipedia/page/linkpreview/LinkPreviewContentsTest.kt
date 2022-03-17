package org.wikipedia.page.linkpreview

import android.text.SpannableStringBuilder
import android.text.style.SuperscriptSpan
import androidx.core.text.getSpans
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite.Companion.forLanguageCode
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.json.JsonUtil
import org.wikipedia.test.TestFileUtil
import org.wikipedia.util.StringUtil.fromHtml

@RunWith(RobolectricTestRunner::class)
class LinkPreviewContentsTest {
    private lateinit var rbPageSummary: PageSummary

    @Before
    @Throws(Throwable::class)
    fun setUp() {
        val json = TestFileUtil.readRawFile("rb_page_summary_valid.json")
        rbPageSummary = JsonUtil.decodeFromString(json)!!
    }

    @Test
    fun testExtractHasSuperscripts() {
        val linkPreviewContents = LinkPreviewContents(rbPageSummary, TEST)
        val extract = fromHtml(linkPreviewContents.extract) as SpannableStringBuilder

        // the 3 <sup> tags in the formula are represented correctly
        MatcherAssert.assertThat(extract.getSpans<SuperscriptSpan>(), Matchers.arrayWithSize(EXPECTED_SUPS))
    }

    companion object {
        private const val EXPECTED_SUPS = 3
        private val TEST = forLanguageCode("test")
    }
}
