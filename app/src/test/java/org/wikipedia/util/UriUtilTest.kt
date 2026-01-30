package org.wikipedia.util

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UriUtilTest {
    @Test
    fun testRemoveFragment() {
        assertEquals(TITLE, UriUtil.removeFragment("$TITLE#$LEGAL_FRAGMENT_CHARS"))
    }

    @Test
    fun testRemoveEmptyFragment() {
        assertEquals(TITLE, UriUtil.removeFragment("$TITLE#"))
    }

    @Test
    fun testRemoveFragmentWithHash() {
        assertEquals(TITLE, UriUtil.removeFragment("$TITLE##"))
    }

    @Test
    fun testEncodeURIComponent() {
        assertEquals("Unencoded", UriUtil.encodeURL("Unencoded"))
        assertEquals("C%2B%2B", UriUtil.encodeURL("C++"))
        assertEquals("100%25", UriUtil.encodeURL("100%"))
        assertEquals("%D0%92%D0%B8%D0%BA%D0%B8%D0%BF%D0%B5%D0%B4%D0%B8%D1%8F", UriUtil.encodeURL("Википедия"))
        assertEquals("Sentence%20with%20spaces", UriUtil.encodeURL("Sentence with spaces"))
    }

    @Test
    fun testDecodeURIComponent() {
        assertEquals("Unencoded", UriUtil.decodeURL("Unencoded"))
        assertEquals("C++", UriUtil.decodeURL("C++"))
        assertEquals("C++", UriUtil.decodeURL("C%2B%2B"))
        assertEquals("Википедия", UriUtil.decodeURL("%D0%92%D0%B8%D0%BA%D0%B8%D0%BF%D0%B5%D0%B4%D0%B8%D1%8F"))
        assertEquals("Sentence with spaces", UriUtil.decodeURL("Sentence%20with%20spaces"))
    }

    @Test
    fun testRemoveInternalLinkPrefix() {
        assertEquals("核武器", UriUtil.removeInternalLinkPrefix("/wiki/核武器"))
        assertEquals("核武器", UriUtil.removeInternalLinkPrefix("/zh/核武器"))
        assertEquals("核武器", UriUtil.removeInternalLinkPrefix("/zh-tw/核武器"))
        assertEquals("核武器", UriUtil.removeInternalLinkPrefix("/zh-hant/核武器"))
        assertEquals("Барак_Обама", UriUtil.removeInternalLinkPrefix("/sr-ec/Барак_Обама"))
        assertEquals("Барак_Обама", UriUtil.removeInternalLinkPrefix("/sr-el/Барак_Обама"))
        assertEquals("Bağçasaray", UriUtil.removeInternalLinkPrefix("/crh-cyrl/Bağçasaray"))
    }

    @Test
    fun testRemoveLinkPrefix() {
        assertEquals("核武器", UriUtil.removeLinkPrefix("https://zh.wikipedia.org/wiki/核武器"))
        assertEquals("核武器", UriUtil.removeLinkPrefix("https://zh.wikipedia.org/zh/核武器"))
        assertEquals("核武器", UriUtil.removeLinkPrefix("https://zh.wikipedia.org/zh-tw/核武器"))
        assertEquals("核武器", UriUtil.removeLinkPrefix("https://zh.wikipedia.org/zh-hant/核武器"))
        assertEquals("核武器", UriUtil.removeLinkPrefix("https://zh-tw.wikipedia.org/wiki/核武器"))
        assertEquals("Барак_Обама", UriUtil.removeLinkPrefix("https://sr.wikipedia.org/sr/Барак_Обама"))
        assertEquals("Барак_Обама", UriUtil.removeLinkPrefix("https://sr.wikipedia.org/sr-el/Барак_Обама"))
        assertEquals("Барак_Обама", UriUtil.removeLinkPrefix("https://sr.wikipedia.org/sr-ec/Барак_Обама"))
    }

    @Test
    fun testFilenameFromUploadUrl() {
        assertEquals("Oaxaca_in_Mexico.svg", UriUtil.getFilenameFromUploadUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/8/83/Oaxaca_in_Mexico.svg/320px-Oaxaca_in_Mexico.svg.png"))
        assertEquals("President_Barack_Obama.jpg", UriUtil.getFilenameFromUploadUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/President_Barack_Obama.jpg/256px-President_Barack_Obama.jpg"))
        assertEquals("President_Barack_Obama.jpg", UriUtil.getFilenameFromUploadUrl("https://upload.wikimedia.org/wikipedia/commons/8/8d/President_Barack_Obama.jpg"))
        assertEquals("Avengers_Endgame_poster.jpg", UriUtil.getFilenameFromUploadUrl("https://upload.wikimedia.org/wikipedia/en/thumb/0/0d/Avengers_Endgame_poster.jpg/216px-Avengers_Endgame_poster.jpg"))
        assertEquals("Needle_Galaxy_4565.jpeg", UriUtil.getFilenameFromUploadUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Needle_Galaxy_4565.jpeg/320px-Needle_Galaxy_4565.jpeg"))
        assertEquals("", UriUtil.getFilenameFromUploadUrl(""))
    }

    @Test
    fun testIsAppSupportedLink() {
        assertTrue(UriUtil.isAppSupportedLink(Uri.parse("https://en.wikipedia.org/wiki/Obama_Barack?markasread=10520073&markasreadwiki=zhwiki")))
        assertTrue(UriUtil.isAppSupportedLink(Uri.parse("https://en.wikipedia.org/w/index.php?title=Spacetime&oldid=prev&diff=816093705&markasread=123759827&markasreadwiki=enwiki")))
        assertTrue(UriUtil.isAppSupportedLink(Uri.parse("https://en.wikipedia.org/wiki/User_talk:Cooltey?markasread=229654787&markasreadwiki=enwiki#c-RSchoenbaechler_(WMF)-2021-10-07T12:18:00.000Z-Cooltey-2021-09-27T22:53:00.000Z")))
        assertFalse(UriUtil.isAppSupportedLink(Uri.parse("https://commons.wikimedia.org/wiki/User_talk:Cooltey?markasread=5393423&markasreadwiki=commonswiki")))
        assertFalse(UriUtil.isAppSupportedLink(Uri.parse("https://mediawiki.org/wiki/Special:MyLanguage/Help:Login_notifications?markasread=135571654&markasreadwiki=enwiki")))
    }

    @Test
    fun testDiffUrl() {
        assertTrue(UriUtil.isDiffUrl("https://en.wikipedia.org/w/index.php?title=User_talk:Android-Test17-WMF&oldid=prev&diff=1147068001"))
        assertFalse(UriUtil.isDiffUrl("https://en.wikipedia.org/wiki/Cat"))
    }

    companion object {
        /**
         * Inspired by
         * curl -s https://en.wikipedia.org/w/api.php?action=query&meta=siteinfo&format=json&siprop=general | jq .query.general.legaltitlechars
         */
        private const val TITLE = " %!\"$&'()*,\\-.\\/0-9:;=?@A-Z\\\\^_`a-z~\\x80-\\xFF+"

        /**
         * Inspired by
         * from http://stackoverflow.com/questions/2849756/list-of-valid-characters-for-the-fragment-identifier-in-an-url
         */
        private const val LEGAL_FRAGMENT_CHARS = "!$&'()*+,;=-._~:@/?abc0123456789%D8%f6"
    }
}
