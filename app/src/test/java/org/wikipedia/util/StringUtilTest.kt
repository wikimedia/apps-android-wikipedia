package org.wikipedia.util

import android.location.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StringUtilTest {
    @Test
    fun testGetBase26String() {
        assertEquals("A", StringUtil.getBase26String(1))
        assertEquals("Z", StringUtil.getBase26String(26))
        assertEquals("JQ", StringUtil.getBase26String(277))
        assertEquals("DBU", StringUtil.getBase26String(2777))
        assertEquals("AMXL", StringUtil.getBase26String(27000))
        assertEquals("AZ", StringUtil.getBase26String(52))
        assertEquals("BA", StringUtil.getBase26String(53))
    }

    @Test
    fun testListToCsv() {
        val stringList: MutableList<String?> = ArrayList()
        assertEquals("", StringUtil.listToCsv(stringList))
        stringList.add("one")
        assertEquals("one", StringUtil.listToCsv(stringList))
        stringList.add("two")
        assertEquals("one,two", StringUtil.listToCsv(stringList))
    }

    @Test
    fun testCsvToList() {
        val stringList: MutableList<String> = ArrayList()
        stringList.add("one")
        stringList.add("two")
        assertEquals(stringList, StringUtil.csvToList("one,two"))
        assertEquals(1, StringUtil.csvToList("one").size)
        assertEquals(0, StringUtil.csvToList("").size)
    }

    @Test
    fun testDelimiterStringToList() {
        val stringList: MutableList<String> = ArrayList()
        stringList.add("one")
        stringList.add("two")
        assertEquals(stringList, StringUtil.delimiterStringToList("one,two", ","))
    }

    @Test
    fun testMd5string() {
        assertEquals("098f6bcd4621d373cade4e832627b4f6", StringUtil.md5string("test"))
        assertEquals("0f28e0cfe175f17806979dff54cc7ea6", StringUtil.md5string("https://en.wikipedia.org/api/rest_v1/page/mobile-html/Earth"))
    }

    @Test
    fun testIntToHexStr() {
        assertEquals("x00000001", StringUtil.intToHexStr(1))
    }

    @Test
    fun testAddUnderscores() {
        assertEquals("te_st", StringUtil.addUnderscores("te st"))
    }

    @Test
    fun testRemoveUnderscores() {
        assertEquals("te st", StringUtil.removeUnderscores("te_st"))
    }

    @Test
    fun testDbNameToLangCode() {
        assertEquals("en", StringUtil.dbNameToLangCode("en"))
        assertEquals("en", StringUtil.dbNameToLangCode("enwiki"))
    }

    @Test
    fun testRemoveSectionAnchor() {
        assertEquals("", StringUtil.removeSectionAnchor("#te_st"))
        assertEquals("sec", StringUtil.removeSectionAnchor("sec#te_st"))
    }

    @Test
    fun testRemoveNamespace() {
        assertEquals("RSP", StringUtil.removeNamespace("RSP"))
        assertEquals("RSP", StringUtil.removeNamespace("WP:RSP"))
    }

    @Test
    fun testRemoveHTMLTags() {
        assertEquals("te_st", StringUtil.removeHTMLTags("<tag>te_st</tag>"))
    }

    @Test
    fun testRemoveStyleTags() {
        assertEquals("Lorem  <i>ipsum</i>", StringUtil.removeStyleTags("Lorem <style data=\"123\">test</style> <i>ipsum</i>"))
    }

    @Test
    fun testRemoveCiteMarkup() {
        assertEquals("Lorem test <i>ipsum</i>", StringUtil.removeCiteMarkup("Lorem <cite data=\"123\">test</cite> <i>ipsum</i>"))
    }

    @Test
    fun testSanitizeAbuseFilterCode() {
        assertEquals("abusefilter-warning-selfpublished", StringUtil.sanitizeAbuseFilterCode("⧼abusefilter-warning-selfpublished⧽"))
    }

    @Test
    fun testGeoHackToLocation() {
        assertNull(StringUtil.geoHackToLocation("test"))
        val location1 = StringUtil.geoHackToLocation("42_N_71_12_13_W")!!
        val location2 = Location("").apply {
            latitude = 42.0
            longitude = 71.0 + 12.0 / 60 + 13.0 / 3600
        }
        assertEquals(location2.latitude, location1.latitude, 0.0)
        assertEquals(location2.longitude, -location1.longitude, 0.0)
    }
}
