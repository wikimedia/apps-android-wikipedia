package org.wikipedia.dataclient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.PageTitle
import org.wikipedia.test.TestParcelUtil

@RunWith(RobolectricTestRunner::class)
class WikiSiteTest {
    @Test
    fun testSupportedAuthority() {
        assertTrue(WikiSite.supportedAuthority("fr.wikipedia.org"))
        assertTrue(WikiSite.supportedAuthority("fr.m.wikipedia.org"))
        assertTrue(WikiSite.supportedAuthority("roa-rup.wikipedia.org"))
        assertFalse(WikiSite.supportedAuthority("google.com"))
    }

    @Test
    fun testForLanguageCodeScheme() {
        val subject = WikiSite.forLanguageCode("test")
        assertEquals("https", subject.scheme())
    }

    @Test
    fun testForLanguageCodeAuthority() {
        val subject = WikiSite.forLanguageCode("test")
        assertEquals("test.wikipedia.org", subject.authority())
    }

    @Test
    fun testForLanguageCodeLanguage() {
        val subject = WikiSite.forLanguageCode("test")
        assertEquals("test", subject.languageCode)
    }

    @Test
    fun testForLanguageCodeNoLanguage() {
        val subject = WikiSite.forLanguageCode("")
        assertEquals("", subject.languageCode)
    }

    @Test
    fun testForLanguageCodeNoLanguageAuthority() {
        val subject = WikiSite.forLanguageCode("")
        assertEquals("wikipedia.org", subject.authority())
    }

    @Test
    fun testForLanguageCodeLanguageAuthority() {
        val subject = WikiSite.forLanguageCode("zh-hans")
        assertEquals("zh.wikipedia.org", subject.authority())
        assertEquals("zh-hans", subject.languageCode)
    }

    @Test
    fun testCtorScheme() {
        val subject = WikiSite("http://wikipedia.org")
        assertEquals("http", subject.scheme())
    }

    @Test
    fun testCtorDefaultScheme() {
        val subject = WikiSite("wikipedia.org")
        assertEquals("https", subject.scheme())
    }

    @Test
    fun testCtorAuthority() {
        val subject = WikiSite("test.wikipedia.org")
        assertEquals("test.wikipedia.org", subject.authority())
    }

    @Test
    fun testCtorAuthorityLanguage() {
        val subject = WikiSite("test.wikipedia.org")
        assertEquals("test", subject.languageCode)
    }

    @Test
    fun testCtorAuthorityNoLanguage() {
        val subject = WikiSite("wikipedia.org")
        assertEquals("", subject.languageCode)
    }

    @Test
    fun testCtordesktopAuthorityLanguage() {
        val subject = WikiSite("test.m.wikipedia.org")
        assertEquals("test", subject.languageCode)
    }

    @Test
    fun testCtordesktopAuthorityNoLanguage() {
        val subject = WikiSite("m.wikipedia.org")
        assertEquals("", subject.languageCode)
    }

    @Test
    fun testCtorUriLangVariant() {
        var subject = WikiSite("en.wikipedia.org/wiki/Foo")
        assertEquals("en.wikipedia.org", subject.authority())
        assertEquals("en", subject.subdomain())
        assertEquals("en", subject.languageCode)
        assertEquals("https", subject.scheme())
        assertEquals("enwiki", subject.dbName())
        assertEquals("https://en.wikipedia.org", subject.url())

        subject = WikiSite("zh.wikipedia.org/zh-tw/Foo")
        assertEquals("zh.wikipedia.org", subject.authority())
        assertEquals("zh", subject.subdomain())
        assertEquals("zh-tw", subject.languageCode)
        assertEquals("https", subject.scheme())
        assertEquals("zhwiki", subject.dbName())
        assertEquals("https://zh.wikipedia.org", subject.url())

        subject = WikiSite("zh.wikipedia.org/zh-cn/Foo")
        assertEquals("zh.wikipedia.org", subject.authority())
        assertEquals("zh", subject.subdomain())
        assertEquals("zh-cn", subject.languageCode)
        assertEquals("https", subject.scheme())
        assertEquals("zhwiki", subject.dbName())
        assertEquals("https://zh.wikipedia.org", subject.url())

        subject = WikiSite("zh.wikipedia.org/zh-hant/Foo")
        assertEquals("zh.wikipedia.org", subject.authority())
        assertEquals("zh", subject.subdomain())
        assertEquals("zh-hant", subject.languageCode)
        assertEquals("https", subject.scheme())
        assertEquals("zhwiki", subject.dbName())
        assertEquals("https://zh.wikipedia.org", subject.url())
    }

    @Test
    fun testCtorUriLangVariantInSubdomain() {
        val subject = WikiSite("zh-tw.wikipedia.org/wiki/Foo")
        assertEquals("zh.wikipedia.org", subject.authority())
        assertEquals("zh", subject.subdomain())
        assertEquals("zh-tw", subject.languageCode)
        assertEquals("https", subject.scheme())
        assertEquals("zhwiki", subject.dbName())
        assertEquals("https://zh.wikipedia.org", subject.url())
    }

    @Test
    fun testCtorMobileUriLangVariant() {
        val subject = WikiSite("zh.m.wikipedia.org/zh-hant/Foo")
        assertEquals("zh.wikipedia.org", subject.authority())
        assertEquals("zh", subject.subdomain())
        assertEquals("zh-hant", subject.languageCode)
        assertEquals("https", subject.scheme())
        assertEquals("https://zh.wikipedia.org", subject.url())
    }

    @Test
    fun testCtorUriNoLangVariant() {
        val subject = WikiSite("http://zh.wikipedia.org/wiki/Foo")
        assertEquals("zh.wikipedia.org", subject.authority())
        assertEquals("zh", subject.subdomain())
        assertEquals("zh", subject.languageCode)
        assertEquals("http", subject.scheme())
        assertEquals("http://zh.wikipedia.org", subject.url())
    }

    @Test
    fun testCtorUriGeneralLangVariant() {
        val subject = WikiSite("http://zh.wikipedia.org/wiki/Foo")
        assertEquals("zh.wikipedia.org", subject.authority())
        assertEquals("zh", subject.subdomain())
        assertEquals("zh", subject.languageCode)
        assertEquals("http", subject.scheme())
        assertEquals("http://zh.wikipedia.org", subject.url())
    }

    @Test
    @Throws(Throwable::class)
    fun testCtorParcel() {
        val subject = WikiSite.forLanguageCode("test")
        TestParcelUtil.test(subject)
    }

    @Test
    fun testAuthority() {
        val subject = WikiSite("test.wikipedia.org", "test")
        assertEquals("test.wikipedia.org", subject.authority())
    }

    @Test
    fun testDesktopAuthorityLanguage() {
        val subject = WikiSite.forLanguageCode("fiu-vro")
        assertEquals("fiu-vro.wikipedia.org", subject.authority())
    }

    @Test
    fun testDesktopAuthorityNoLanguage() {
        val subject = WikiSite("wikipedia.org")
        assertEquals("wikipedia.org", subject.authority())
    }

    @Test
    fun testDesktopAuthorityLanguageAuthority() {
        val subject = WikiSite("no.wikipedia.org", "nb")
        assertEquals("no.wikipedia.org", subject.authority())
    }

    @Test
    fun testDesktopAuthoritydesktopAuthority() {
        val subject = WikiSite("ru.wikipedia.org")
        assertEquals("ru.wikipedia.org", subject.authority())
    }

    @Test
    fun testDbNameLanguage() {
        val subject = WikiSite("en.wikipedia.org", "en")
        assertEquals("enwiki", subject.dbName())
    }

    @Test
    fun testDbNameSpecialLanguage() {
        val subject = WikiSite("no.wikipedia.org", "nb")
        assertEquals("nowiki", subject.dbName())
    }

    @Test
    fun testDbNameWithOneUnderscore() {
        val subject = WikiSite("zh-yue.wikipedia.org")
        assertEquals("zh_yuewiki", subject.dbName())
    }

    @Test
    fun testDbNameWithTwoUnderscore() {
        val subject = WikiSite("zh-min-nan.wikipedia.org")
        assertEquals("zh_min_nanwiki", subject.dbName())
    }

    @Test
    fun testPath() {
        val subject = WikiSite.forLanguageCode("test")
        assertEquals("/w/Segment", subject.path("Segment"))
    }

    @Test
    fun testPathEmpty() {
        val subject = WikiSite.forLanguageCode("test")
        assertEquals("/w/", subject.path(""))
    }

    @Test
    fun testUrl() {
        val subject = WikiSite("test.192.168.1.11:8080", "test")
        assertEquals("https://test.192.168.1.11:8080", subject.url())
    }

    @Test
    fun testUrlPath() {
        val subject = WikiSite.forLanguageCode("test")
        assertEquals("https://test.wikipedia.org/w/Segment", subject.url("Segment"))
    }

    @Test
    fun testLanguageCode() {
        val subject = WikiSite.forLanguageCode("lang")
        assertEquals("lang", subject.languageCode)
    }

    @Test
    fun testUnmarshal() {
        val wiki = WikiSite.forLanguageCode("test")
        val wiki2 = JsonUtil.decodeFromString<WikiSite>(JsonUtil.encodeToString(wiki))!!
        assertEquals(wiki.languageCode, wiki2.languageCode)
        assertEquals(wiki.uri, wiki2.uri)
    }

    @Test
    fun testUnmarshalScheme() {
        val wiki = WikiSite("wikipedia.org", "")
        val wiki2 = JsonUtil.decodeFromString<WikiSite>(JsonUtil.encodeToString(wiki))!!
        assertEquals(wiki.languageCode, wiki2.languageCode)
        assertEquals(wiki.uri, wiki2.uri)
    }

    @Test
    fun testTitleForInternalLink() {
        val wiki = WikiSite.forLanguageCode("en")
        assertEquals(
            PageTitle("Main Page", wiki).prefixedText,
            PageTitle.titleForInternalLink(null, wiki).prefixedText
        )
        assertEquals(
            PageTitle("Main Page", wiki).prefixedText,
            PageTitle.titleForInternalLink("", wiki).prefixedText
        )
        assertEquals(
            PageTitle("Main Page", wiki).prefixedText,
            PageTitle.titleForInternalLink("/wiki/", wiki).prefixedText
        )
        assertEquals(
            PageTitle("wiki", wiki).prefixedText,
            PageTitle.titleForInternalLink("wiki", wiki).prefixedText
        )
        assertEquals(
            PageTitle("wiki", wiki).prefixedText,
            PageTitle.titleForInternalLink("/wiki/wiki", wiki).prefixedText
        )
        assertEquals(
            PageTitle("wiki/wiki", wiki).prefixedText,
            PageTitle.titleForInternalLink("/wiki/wiki/wiki", wiki).prefixedText
        )
    }

    @Test
    fun testEquals() {
        assertEquals(WikiSite.forLanguageCode("en"), WikiSite.forLanguageCode("en"))
        assertNotEquals(WikiSite.forLanguageCode("ta"), WikiSite.forLanguageCode("en"))
        assertFalse(WikiSite.forLanguageCode("ta").equals("ta.wikipedia.org"))
    }

    @Test
    fun testNormalization() {
        assertEquals("bm.wikipedia.org", WikiSite.forLanguageCode("bm").authority())
    }
}
