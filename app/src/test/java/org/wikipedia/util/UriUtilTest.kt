package org.wikipedia.util

import android.net.Uri
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UriUtilTest {
    @Test
    fun testRemoveFragment() {
        MatcherAssert.assertThat(UriUtil.removeFragment("$TITLE#$LEGAL_FRAGMENT_CHARS"), Matchers.`is`(TITLE))
    }

    @Test
    fun testRemoveEmptyFragment() {
        MatcherAssert.assertThat(UriUtil.removeFragment("$TITLE#"), Matchers.`is`(TITLE))
    }

    @Test
    fun testRemoveFragmentWithHash() {
        MatcherAssert.assertThat(UriUtil.removeFragment("$TITLE##"), Matchers.`is`(TITLE))
    }

    @Test
    fun testEncodeURIComponent() {
        MatcherAssert.assertThat(UriUtil.encodeURL("Unencoded"), Matchers.`is`("Unencoded"))
        MatcherAssert.assertThat(UriUtil.encodeURL("C++"), Matchers.`is`("C%2B%2B"))
        MatcherAssert.assertThat(UriUtil.encodeURL("100%"), Matchers.`is`("100%25"))
        MatcherAssert.assertThat(UriUtil.encodeURL("Википедия"), Matchers.`is`("%D0%92%D0%B8%D0%BA%D0%B8%D0%BF%D0%B5%D0%B4%D0%B8%D1%8F"))
        MatcherAssert.assertThat(UriUtil.encodeURL("Sentence with spaces"), Matchers.`is`("Sentence%20with%20spaces"))
    }

    @Test
    fun testDecodeURIComponent() {
        MatcherAssert.assertThat(UriUtil.decodeURL("Unencoded"), Matchers.`is`("Unencoded"))
        MatcherAssert.assertThat(UriUtil.decodeURL("C++"), Matchers.`is`("C++"))
        MatcherAssert.assertThat(UriUtil.decodeURL("C%2B%2B"), Matchers.`is`("C++"))
        MatcherAssert.assertThat(UriUtil.decodeURL("%D0%92%D0%B8%D0%BA%D0%B8%D0%BF%D0%B5%D0%B4%D0%B8%D1%8F"), Matchers.`is`("Википедия"))
        MatcherAssert.assertThat(UriUtil.decodeURL("Sentence%20with%20spaces"), Matchers.`is`("Sentence with spaces"))
    }

    @Test
    fun testRemoveInternalLinkPrefix() {
        MatcherAssert.assertThat(UriUtil.removeInternalLinkPrefix("/wiki/核武器"), Matchers.`is`("核武器"))
        MatcherAssert.assertThat(UriUtil.removeInternalLinkPrefix("/zh/核武器"), Matchers.`is`("核武器"))
        MatcherAssert.assertThat(UriUtil.removeInternalLinkPrefix("/zh-tw/核武器"), Matchers.`is`("核武器"))
        MatcherAssert.assertThat(UriUtil.removeInternalLinkPrefix("/zh-hant/核武器"), Matchers.`is`("核武器"))
        MatcherAssert.assertThat(UriUtil.removeInternalLinkPrefix("/sr-ec/Барак_Обама"), Matchers.`is`("Барак_Обама"))
        MatcherAssert.assertThat(UriUtil.removeInternalLinkPrefix("/sr-el/Барак_Обама"), Matchers.`is`("Барак_Обама"))
        MatcherAssert.assertThat(UriUtil.removeInternalLinkPrefix("/crh-cyrl/Bağçasaray"), Matchers.`is`("Bağçasaray"))
    }

    @Test
    fun testRemoveLinkPrefix() {
        MatcherAssert.assertThat(UriUtil.removeLinkPrefix("https://zh.wikipedia.org/wiki/核武器"), Matchers.`is`("核武器"))
        MatcherAssert.assertThat(UriUtil.removeLinkPrefix("https://zh.wikipedia.org/zh/核武器"), Matchers.`is`("核武器"))
        MatcherAssert.assertThat(UriUtil.removeLinkPrefix("https://zh.wikipedia.org/zh-tw/核武器"), Matchers.`is`("核武器"))
        MatcherAssert.assertThat(UriUtil.removeLinkPrefix("https://zh.wikipedia.org/zh-hant/核武器"), Matchers.`is`("核武器"))
        MatcherAssert.assertThat(UriUtil.removeLinkPrefix("https://zh-tw.wikipedia.org/wiki/核武器"), Matchers.`is`("核武器"))
        MatcherAssert.assertThat(UriUtil.removeLinkPrefix("https://sr.wikipedia.org/sr/Барак_Обама"), Matchers.`is`("Барак_Обама"))
        MatcherAssert.assertThat(UriUtil.removeLinkPrefix("https://sr.wikipedia.org/sr-el/Барак_Обама"), Matchers.`is`("Барак_Обама"))
        MatcherAssert.assertThat(UriUtil.removeLinkPrefix("https://sr.wikipedia.org/sr-ec/Барак_Обама"), Matchers.`is`("Барак_Обама"))
    }

    @Test
    fun testFilenameFromUploadUrl() {
        MatcherAssert.assertThat(UriUtil.getFilenameFromUploadUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/8/83/Oaxaca_in_Mexico.svg/320px-Oaxaca_in_Mexico.svg.png"), Matchers.`is`("Oaxaca_in_Mexico.svg"))
        MatcherAssert.assertThat(UriUtil.getFilenameFromUploadUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/President_Barack_Obama.jpg/256px-President_Barack_Obama.jpg"), Matchers.`is`("President_Barack_Obama.jpg"))
        MatcherAssert.assertThat(UriUtil.getFilenameFromUploadUrl("https://upload.wikimedia.org/wikipedia/commons/8/8d/President_Barack_Obama.jpg"), Matchers.`is`("President_Barack_Obama.jpg"))
        MatcherAssert.assertThat(UriUtil.getFilenameFromUploadUrl("https://upload.wikimedia.org/wikipedia/en/thumb/0/0d/Avengers_Endgame_poster.jpg/216px-Avengers_Endgame_poster.jpg"), Matchers.`is`("Avengers_Endgame_poster.jpg"))
        MatcherAssert.assertThat(UriUtil.getFilenameFromUploadUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Needle_Galaxy_4565.jpeg/320px-Needle_Galaxy_4565.jpeg"), Matchers.`is`("Needle_Galaxy_4565.jpeg"))
        MatcherAssert.assertThat(UriUtil.getFilenameFromUploadUrl(""), Matchers.`is`(""))
    }

    @Test
    fun testParseTalkTopicFromFragment() {
        MatcherAssert.assertThat(UriUtil.parseTalkTopicFromFragment("c-Dmitry_Brant-2021-10-01T12:36:00.000Z-test"), Matchers.`is`("test"))
        MatcherAssert.assertThat(UriUtil.parseTalkTopicFromFragment("c-Dmitry_Brant-2021-10-01T12:36:00.000Z-test-1-2-3"), Matchers.`is`("test-1-2-3"))
        MatcherAssert.assertThat(UriUtil.parseTalkTopicFromFragment("test"), Matchers.`is`("test"))
    }

    @Test
    fun testIsAppSupportedLink() {
        MatcherAssert.assertThat(UriUtil.isAppSupportedLink(Uri.parse("https://en.wikipedia.org/wiki/Obama_Barack?markasread=10520073&markasreadwiki=zhwiki")), Matchers.`is`(true))
        MatcherAssert.assertThat(UriUtil.isAppSupportedLink(Uri.parse("https://en.wikipedia.org/w/index.php?title=Spacetime&oldid=prev&diff=816093705&markasread=123759827&markasreadwiki=enwiki")), Matchers.`is`(true))
        MatcherAssert.assertThat(UriUtil.isAppSupportedLink(Uri.parse("https://en.wikipedia.org/wiki/User_talk:Cooltey?markasread=229654787&markasreadwiki=enwiki#c-RSchoenbaechler_(WMF)-2021-10-07T12:18:00.000Z-Cooltey-2021-09-27T22:53:00.000Z")), Matchers.`is`(true))
        MatcherAssert.assertThat(UriUtil.isAppSupportedLink(Uri.parse("https://commons.wikimedia.org/wiki/User_talk:Cooltey?markasread=5393423&markasreadwiki=commonswiki")), Matchers.`is`(false))
        MatcherAssert.assertThat(UriUtil.isAppSupportedLink(Uri.parse("https://mediawiki.org/wiki/Special:MyLanguage/Help:Login_notifications?markasread=135571654&markasreadwiki=enwiki")), Matchers.`is`(false))
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
