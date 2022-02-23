package org.wikipedia.util

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StringUtilTest {
    @Test
    fun testGetBase26String() {
        MatcherAssert.assertThat(StringUtil.getBase26String(1), Matchers.`is`("A"))
        MatcherAssert.assertThat(StringUtil.getBase26String(26), Matchers.`is`("Z"))
        MatcherAssert.assertThat(StringUtil.getBase26String(277), Matchers.`is`("JQ"))
        MatcherAssert.assertThat(StringUtil.getBase26String(2777), Matchers.`is`("DBU"))
        MatcherAssert.assertThat(StringUtil.getBase26String(27000), Matchers.`is`("AMXL"))
        MatcherAssert.assertThat(StringUtil.getBase26String(52), Matchers.`is`("AZ"))
        MatcherAssert.assertThat(StringUtil.getBase26String(53), Matchers.`is`("BA"))
    }

    @Test
    fun testListToCsv() {
        val stringList: MutableList<String?> = ArrayList()
        MatcherAssert.assertThat(StringUtil.listToCsv(stringList), Matchers.`is`(""))
        stringList.add("one")
        MatcherAssert.assertThat(StringUtil.listToCsv(stringList), Matchers.`is`("one"))
        stringList.add("two")
        MatcherAssert.assertThat(StringUtil.listToCsv(stringList), Matchers.`is`("one,two"))
    }

    @Test
    fun testCsvToList() {
        val stringList: MutableList<String> = ArrayList()
        stringList.add("one")
        stringList.add("two")
        MatcherAssert.assertThat(StringUtil.csvToList("one,two"), Matchers.`is`(stringList))
        MatcherAssert.assertThat(StringUtil.csvToList("one").size, Matchers.`is`(1))
        MatcherAssert.assertThat(StringUtil.csvToList("").size, Matchers.`is`(0))
    }

    @Test
    fun testDelimiterStringToList() {
        val stringList: MutableList<String> = ArrayList()
        stringList.add("one")
        stringList.add("two")
        MatcherAssert.assertThat(StringUtil.delimiterStringToList("one,two", ","), Matchers.`is`(stringList))
    }

    @Test
    fun testMd5string() {
        MatcherAssert.assertThat(StringUtil.md5string("test"), Matchers.`is`("098f6bcd4621d373cade4e832627b4f6"))
        MatcherAssert.assertThat(StringUtil.md5string("https://en.wikipedia.org/api/rest_v1/page/mobile-html/Earth"), Matchers.`is`("0f28e0cfe175f17806979dff54cc7ea6"))
    }

    @Test
    fun testStrip() {
        MatcherAssert.assertThat(StringUtil.strip("test"), Matchers.`is`("test"))
        MatcherAssert.assertThat(StringUtil.strip(" test  "), Matchers.`is`("test"))
    }

    @Test
    fun testIntToHexStr() {
        MatcherAssert.assertThat(StringUtil.intToHexStr(1), Matchers.`is`("x00000001"))
    }

    @Test
    fun testAddUnderscores() {
        MatcherAssert.assertThat(StringUtil.addUnderscores("te st"), Matchers.`is`("te_st"))
    }

    @Test
    fun testRemoveUnderscores() {
        MatcherAssert.assertThat(StringUtil.removeUnderscores("te_st"), Matchers.`is`("te st"))
    }

    @Test
    fun testRemoveSectionAnchor() {
        MatcherAssert.assertThat(StringUtil.removeSectionAnchor("#te_st"), Matchers.`is`(""))
        MatcherAssert.assertThat(StringUtil.removeSectionAnchor("sec#te_st"), Matchers.`is`("sec"))
    }

    @Test
    fun testRemoveHTMLTags() {
        MatcherAssert.assertThat(StringUtil.removeHTMLTags("<tag>te_st</tag>"), Matchers.`is`("te_st"))
    }

    @Test
    fun testRemoveStyleTags() {
        MatcherAssert.assertThat(StringUtil.removeStyleTags("Lorem <style data=\"123\">test</style> <i>ipsum</i>"), Matchers.`is`("Lorem  <i>ipsum</i>"))
    }

    @Test
    fun testRemoveCiteMarkup() {
        MatcherAssert.assertThat(StringUtil.removeCiteMarkup("Lorem <cite data=\"123\">test</cite> <i>ipsum</i>"), Matchers.`is`("Lorem test <i>ipsum</i>"))
    }

    @Test
    fun testSanitizeAbuseFilterCode() {
        MatcherAssert.assertThat(StringUtil.sanitizeAbuseFilterCode("⧼abusefilter-warning-selfpublished⧽"), Matchers.`is`("abusefilter-warning-selfpublished"))
    }
}
