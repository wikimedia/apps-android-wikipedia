package org.wikipedia.page

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.staticdata.MainPageNameData

@RunWith(RobolectricTestRunner::class)
class PageTitleTest {
    @Test
    fun testPrefixedText() {
        val enwiki = WikiSite.forLanguageCode("en")

        assertEquals("Test__title", PageTitle(null, "Test  title", enwiki).prefixedText)
        assertEquals("Test_title", PageTitle(null, "Test title", enwiki).prefixedText)
        assertEquals("Talk:Test_title", PageTitle("Talk", "Test title", enwiki).prefixedText)
        assertEquals("Test_title", PageTitle(null, "Test title", enwiki).text)
    }

    @Test
    fun testFromInternalLink() {
        val enwiki = WikiSite.forLanguageCode("en")

        assertEquals("India", PageTitle.titleForInternalLink("/wiki/India", enwiki).prefixedText)
        assertEquals("", PageTitle.titleForInternalLink("/wiki/India", enwiki).namespace)
        assertEquals("Talk", PageTitle.titleForInternalLink("/wiki/Talk:India", enwiki).namespace)
        assertEquals("India", PageTitle.titleForInternalLink("/wiki/Talk:India", enwiki).text)
        assertNull(PageTitle.titleForInternalLink("/wiki/Talk:India", enwiki).fragment)
        assertEquals("Talk", PageTitle.titleForInternalLink("/wiki/Talk:India#", enwiki).namespace)
        assertEquals("India", PageTitle.titleForInternalLink("/wiki/Talk:India#", enwiki).text)
        assertNull(PageTitle.titleForInternalLink("/wiki/Talk:India#", enwiki).fragment)
        assertEquals("Talk", PageTitle.titleForInternalLink("/wiki/Talk:India#History", enwiki).namespace)
        assertEquals("India", PageTitle.titleForInternalLink("/wiki/Talk:India#History", enwiki).text)
        assertEquals("History", PageTitle.titleForInternalLink("/wiki/Talk:India#History", enwiki).fragment)
    }

    @Test
    fun testCanonicalURL() {
        val enwiki = WikiSite.forLanguageCode("en")

        assertEquals("https://en.wikipedia.org/wiki/India", PageTitle.titleForInternalLink("/wiki/India", enwiki).uri)
        assertEquals("https://en.wikipedia.org/wiki/India_Gate", PageTitle.titleForInternalLink("/wiki/India Gate", enwiki).uri)
        assertEquals("https://en.wikipedia.org/wiki/India%27s_Gate", PageTitle.titleForInternalLink("/wiki/India's Gate", enwiki).uri)
    }

    @Test
    fun testVariants() {
        val zhwiki = WikiSite.forLanguageCode("zh-tw")

        assertEquals("https://en.wikipedia.org/wiki/Taiwan", PageTitle("Taiwan", WikiSite.forLanguageCode("en")).uri)
        assertEquals("https://zh.wikipedia.org/zh/Taiwan", PageTitle("Taiwan", WikiSite.forLanguageCode("zh")).uri)
        assertEquals("https://zh.wikipedia.org/zh-tw/Taiwan", PageTitle("Taiwan", WikiSite.forLanguageCode("zh-tw")).uri)
        assertEquals("https://zh.wikipedia.org/zh-cn/Taiwan", PageTitle("Taiwan", WikiSite.forLanguageCode("zh-cn")).uri)
        assertEquals("https://zh.wikipedia.org/zh-tw/Taiwan", PageTitle.titleForInternalLink("/zh/Taiwan", zhwiki).uri)
        assertEquals("https://zh.wikipedia.org/zh-tw/Taiwan", PageTitle.titleForInternalLink("/zh-tw/Taiwan", zhwiki).uri)
        assertEquals("https://zh.wikipedia.org/zh-tw/Taiwan", PageTitle.titleForInternalLink("/wiki/Taiwan", zhwiki).uri)
    }

    @Test
    fun testWikiSite() {
        val enwiki = WikiSite.forLanguageCode("en")
        assertEquals(enwiki, PageTitle(null, "Test", enwiki).wikiSite)
        assertEquals(enwiki, WikiSite.forLanguageCode("en"))
    }

    @Test
    fun testLangAsNamespace() {
        val uri = Uri.parse("https://en.wikipedia.org/wiki/fr:Article")
        val site = WikiSite(uri)
        val title = PageTitle.titleForUri(uri, site)
        assertEquals("fr.wikipedia.org", title.wikiSite.authority())
        assertEquals("Article", title.displayText)
    }

    @Test
    fun testParsing() {
        val enwiki = WikiSite.forLanguageCode("en")

        assertEquals("Hello", PageTitle("Hello", enwiki).displayText)
        assertEquals("Talk:Hello", PageTitle("Talk:Hello", enwiki).displayText)
        assertEquals("Hello", PageTitle("Talk:Hello", enwiki).text)
        assertEquals("Talk", PageTitle("Talk:Hello", enwiki).namespace)
        assertEquals("Wikipedia talk:Hello world", PageTitle("Wikipedia_talk:Hello world", enwiki).displayText)
    }

    @Test
    fun testSpecial() {
        assertTrue(PageTitle("Special:Version", WikiSite.forLanguageCode("en")).isSpecial)
        assertTrue(PageTitle("特別:Version", WikiSite.forLanguageCode("ja")).isSpecial)
        assertTrue(PageTitle("Special:Version", WikiSite.forLanguageCode("ja")).isSpecial)
        assertFalse(PageTitle("特別:Version", WikiSite.forLanguageCode("en")).isSpecial)
    }

    @Test
    fun testFile() {
        assertTrue(PageTitle("File:SomethingSomething", WikiSite.forLanguageCode("en")).isFilePage)
        assertTrue(PageTitle("ファイル:Version", WikiSite.forLanguageCode("ja")).isFilePage)
        assertTrue(PageTitle("File:SomethingSomething", WikiSite.forLanguageCode("ja")).isFilePage)
        assertFalse(PageTitle("ファイル:Version", WikiSite.forLanguageCode("en")).isFilePage)
    }

    @Test
    fun testMainPage() {
        val enwiki = WikiSite.forLanguageCode("en")
        assertEquals(PageTitle(MainPageNameData.valueFor("en"), enwiki).prefixedText,
            PageTitle("", enwiki).prefixedText)
    }

    @Test
    fun testIsMainPageNoTitleNoProps() {
        val text: String? = null
        val wiki = WikiSite.forLanguageCode("test")
        val subject = PageTitle(text, wiki)

        assertTrue(subject.isMainPage)
    }

    @Test
    fun testIsMainPageTitleNoProps() {
        val text = "text"
        val wiki = WikiSite.forLanguageCode("test")
        val subject = PageTitle(text, wiki)

        assertFalse(subject.isMainPage)
    }

    /** https://bugzilla.wikimedia.org/66151  */
    @Test
    fun testHashChar() {
        val pageTitle = PageTitle("#", WikiSite.forLanguageCode("en"))
        assertEquals("", pageTitle.namespace)
        assertEquals("", pageTitle.text)
        assertNull(pageTitle.fragment)
    }

    @Test
    fun testColonChar() {
        val pageTitle = PageTitle(":", WikiSite.forLanguageCode("en"))
        assertEquals("", pageTitle.namespace)
        assertEquals(":", pageTitle.text)
        assertNull(pageTitle.fragment)
    }

    @Test
    fun testEquality() {
        val title1 = PageTitle("Test Article", WikiSite.forLanguageCode("en"))
        var title2 = PageTitle("Test Article", WikiSite.forLanguageCode("en"))
        assertEquals(title1, title2)
        assertEquals(title2, title1)
        title2.displayText = "TEST ARTICLE"
        title2.thumbUrl = "http://thumb.url"
        title2.extract = "Foo"
        title2.description = "Bar"
        assertEquals(title1, title2)
        assertEquals(title2, title1)
        title2.namespace = "File"
        assertNotEquals(title1, title2)
        assertNotEquals(title2, title1)

        title2 = PageTitle("Test Article", WikiSite.forLanguageCode("ru"))
        assertNotEquals(title1, title2)
        assertNotEquals(title2, title1)

        assertNotEquals(title1, Any())
    }
}
