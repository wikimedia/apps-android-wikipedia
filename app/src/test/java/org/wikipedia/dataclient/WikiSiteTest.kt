package org.wikipedia.dataclient

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.page.PageTitle
import org.wikipedia.test.TestParcelUtil

@RunWith(RobolectricTestRunner::class)
class WikiSiteTest {
    @Test
    fun testSupportedAuthority() {
        MatcherAssert.assertThat(WikiSite.supportedAuthority("fr.wikipedia.org"), Matchers.`is`(true))
        MatcherAssert.assertThat(WikiSite.supportedAuthority("fr.m.wikipedia.org"), Matchers.`is`(true))
        MatcherAssert.assertThat(WikiSite.supportedAuthority("roa-rup.wikipedia.org"), Matchers.`is`(true))
        MatcherAssert.assertThat(WikiSite.supportedAuthority("google.com"), Matchers.`is`(false))
    }

    @Test
    fun testForLanguageCodeScheme() {
        val subject = WikiSite.forLanguageCode("test")
        MatcherAssert.assertThat(subject.scheme(), Matchers.`is`("https"))
    }

    @Test
    fun testForLanguageCodeAuthority() {
        val subject = WikiSite.forLanguageCode("test")
        MatcherAssert.assertThat(subject.authority(), Matchers.`is`("test.wikipedia.org"))
    }

    @Test
    fun testForLanguageCodeLanguage() {
        val subject = WikiSite.forLanguageCode("test")
        MatcherAssert.assertThat(subject.languageCode, Matchers.`is`("test"))
    }

    @Test
    fun testForLanguageCodeNoLanguage() {
        val subject = WikiSite.forLanguageCode("")
        MatcherAssert.assertThat(subject.languageCode, Matchers.`is`(""))
    }

    @Test
    fun testForLanguageCodeNoLanguageAuthority() {
        val subject = WikiSite.forLanguageCode("")
        MatcherAssert.assertThat(subject.authority(), Matchers.`is`("wikipedia.org"))
    }

    @Test
    fun testForLanguageCodeLanguageAuthority() {
        val subject = WikiSite.forLanguageCode("zh-hans")
        MatcherAssert.assertThat(subject.authority(), Matchers.`is`("zh.wikipedia.org"))
        MatcherAssert.assertThat(subject.languageCode, Matchers.`is`("zh-hans"))
    }

    @Test
    fun testCtorScheme() {
        val subject = WikiSite("http://wikipedia.org")
        MatcherAssert.assertThat(subject.scheme(), Matchers.`is`("http"))
    }

    @Test
    fun testCtorDefaultScheme() {
        val subject = WikiSite("wikipedia.org")
        MatcherAssert.assertThat(subject.scheme(), Matchers.`is`("https"))
    }

    @Test
    fun testCtorAuthority() {
        val subject = WikiSite("test.wikipedia.org")
        MatcherAssert.assertThat(subject.authority(), Matchers.`is`("test.wikipedia.org"))
    }

    @Test
    fun testCtorAuthorityLanguage() {
        val subject = WikiSite("test.wikipedia.org")
        MatcherAssert.assertThat(subject.languageCode, Matchers.`is`("test"))
    }

    @Test
    fun testCtorAuthorityNoLanguage() {
        val subject = WikiSite("wikipedia.org")
        MatcherAssert.assertThat(subject.languageCode, Matchers.`is`(""))
    }

    @Test
    fun testCtordesktopAuthorityLanguage() {
        val subject = WikiSite("test.m.wikipedia.org")
        MatcherAssert.assertThat(subject.languageCode, Matchers.`is`("test"))
    }

    @Test
    fun testCtordesktopAuthorityNoLanguage() {
        val subject = WikiSite("m.wikipedia.org")
        MatcherAssert.assertThat(subject.languageCode, Matchers.`is`(""))
    }

    @Test
    fun testCtorUriLangVariant() {
        val subject = WikiSite("zh.wikipedia.org/zh-hant/Foo")
        MatcherAssert.assertThat(subject.authority(), Matchers.`is`("zh.wikipedia.org"))
        MatcherAssert.assertThat(subject.subdomain(), Matchers.`is`("zh"))
        MatcherAssert.assertThat(subject.languageCode, Matchers.`is`("zh-hant"))
        MatcherAssert.assertThat(subject.scheme(), Matchers.`is`("https"))
        MatcherAssert.assertThat(subject.dbName(), Matchers.`is`("zhwiki"))
        MatcherAssert.assertThat(subject.url(), Matchers.`is`("https://zh.wikipedia.org"))
    }

    @Test
    fun testCtorUriLangVariantInSubdomain() {
        val subject = WikiSite("zh-tw.wikipedia.org/wiki/Foo")
        MatcherAssert.assertThat(subject.authority(), Matchers.`is`("zh.wikipedia.org"))
        MatcherAssert.assertThat(subject.subdomain(), Matchers.`is`("zh"))
        MatcherAssert.assertThat(subject.languageCode, Matchers.`is`("zh-tw"))
        MatcherAssert.assertThat(subject.scheme(), Matchers.`is`("https"))
        MatcherAssert.assertThat(subject.dbName(), Matchers.`is`("zhwiki"))
        MatcherAssert.assertThat(subject.url(), Matchers.`is`("https://zh.wikipedia.org"))
    }

    @Test
    fun testCtorMobileUriLangVariant() {
        val subject = WikiSite("zh.m.wikipedia.org/zh-hant/Foo")
        MatcherAssert.assertThat(subject.authority(), Matchers.`is`("zh.wikipedia.org"))
        MatcherAssert.assertThat(subject.subdomain(), Matchers.`is`("zh"))
        MatcherAssert.assertThat(subject.languageCode, Matchers.`is`("zh-hant"))
        MatcherAssert.assertThat(subject.scheme(), Matchers.`is`("https"))
        MatcherAssert.assertThat(subject.url(), Matchers.`is`("https://zh.wikipedia.org"))
    }

    @Test
    fun testCtorUriNoLangVariant() {
        val subject = WikiSite("http://zh.wikipedia.org/wiki/Foo")
        MatcherAssert.assertThat(subject.authority(), Matchers.`is`("zh.wikipedia.org"))
        MatcherAssert.assertThat(subject.subdomain(), Matchers.`is`("zh"))
        MatcherAssert.assertThat(subject.languageCode, Matchers.`is`("zh-hant"))
        MatcherAssert.assertThat(subject.scheme(), Matchers.`is`("http"))
        MatcherAssert.assertThat(subject.url(), Matchers.`is`("http://zh.wikipedia.org"))
    }

    @Test
    fun testCtorUriGeneralLangVariant() {
        val subject = WikiSite("http://zh.wikipedia.org/wiki/Foo")
        MatcherAssert.assertThat(subject.authority(), Matchers.`is`("zh.wikipedia.org"))
        MatcherAssert.assertThat(subject.subdomain(), Matchers.`is`("zh"))
        MatcherAssert.assertThat(subject.languageCode, Matchers.`is`("zh-hant"))
        MatcherAssert.assertThat(subject.scheme(), Matchers.`is`("http"))
        MatcherAssert.assertThat(subject.url(), Matchers.`is`("http://zh.wikipedia.org"))
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
        MatcherAssert.assertThat(subject.authority(), Matchers.`is`("test.wikipedia.org"))
    }

    @Test
    fun testDesktopAuthorityLanguage() {
        val subject = WikiSite.forLanguageCode("fiu-vro")
        MatcherAssert.assertThat(subject.authority(), Matchers.`is`("fiu-vro.wikipedia.org"))
    }

    @Test
    fun testDesktopAuthorityNoLanguage() {
        val subject = WikiSite("wikipedia.org")
        MatcherAssert.assertThat(subject.authority(), Matchers.`is`("wikipedia.org"))
    }

    @Test
    fun testDesktopAuthorityLanguageAuthority() {
        val subject = WikiSite("no.wikipedia.org", "nb")
        MatcherAssert.assertThat(subject.authority(), Matchers.`is`("no.wikipedia.org"))
    }

    @Test
    fun testDesktopAuthoritydesktopAuthority() {
        val subject = WikiSite("ru.wikipedia.org")
        MatcherAssert.assertThat(subject.authority(), Matchers.`is`("ru.wikipedia.org"))
    }

    @Test
    fun testDbNameLanguage() {
        val subject = WikiSite("en.wikipedia.org", "en")
        MatcherAssert.assertThat(subject.dbName(), Matchers.`is`("enwiki"))
    }

    @Test
    fun testDbNameSpecialLanguage() {
        val subject = WikiSite("no.wikipedia.org", "nb")
        MatcherAssert.assertThat(subject.dbName(), Matchers.`is`("nowiki"))
    }

    @Test
    fun testDbNameWithOneUnderscore() {
        val subject = WikiSite("zh-yue.wikipedia.org")
        MatcherAssert.assertThat(subject.dbName(), Matchers.`is`("zh_yuewiki"))
    }

    @Test
    fun testDbNameWithTwoUnderscore() {
        val subject = WikiSite("zh-min-nan.wikipedia.org")
        MatcherAssert.assertThat(subject.dbName(), Matchers.`is`("zh_min_nanwiki"))
    }

    @Test
    fun testPath() {
        val subject = WikiSite.forLanguageCode("test")
        MatcherAssert.assertThat(subject.path("Segment"), Matchers.`is`("/w/Segment"))
    }

    @Test
    fun testPathEmpty() {
        val subject = WikiSite.forLanguageCode("test")
        MatcherAssert.assertThat(subject.path(""), Matchers.`is`("/w/"))
    }

    @Test
    fun testUrl() {
        val subject = WikiSite("test.192.168.1.11:8080", "test")
        MatcherAssert.assertThat(subject.url(), Matchers.`is`("https://test.192.168.1.11:8080"))
    }

    @Test
    fun testUrlPath() {
        val subject = WikiSite.forLanguageCode("test")
        MatcherAssert.assertThat(
            subject.url("Segment"),
            Matchers.`is`("https://test.wikipedia.org/w/Segment")
        )
    }

    @Test
    fun testLanguageCode() {
        val subject = WikiSite.forLanguageCode("lang")
        MatcherAssert.assertThat(subject.languageCode, Matchers.`is`("lang"))
    }

    @Test
    fun testUnmarshal() {
        val wiki = WikiSite.forLanguageCode("test")
        val wiki2 = GsonUnmarshaller.unmarshal(WikiSite::class.java, GsonMarshaller.marshal(wiki))
        MatcherAssert.assertThat(wiki2.languageCode, Matchers.`is`(wiki.languageCode))
        MatcherAssert.assertThat(wiki2.uri, Matchers.`is`(wiki.uri))
    }

    @Test
    fun testUnmarshalScheme() {
        val wiki = WikiSite("wikipedia.org", "")
        val wiki2 = GsonUnmarshaller.unmarshal(WikiSite::class.java, GsonMarshaller.marshal(wiki))
        MatcherAssert.assertThat(wiki2.languageCode, Matchers.`is`(wiki.languageCode))
        MatcherAssert.assertThat(wiki2.uri, Matchers.`is`(wiki.uri))
    }

    @Test
    fun testTitleForInternalLink() {
        val wiki = WikiSite.forLanguageCode("en")
        MatcherAssert.assertThat(
            PageTitle("Main Page", wiki).prefixedText,
            Matchers.`is`(wiki.titleForInternalLink(null).prefixedText)
        )
        MatcherAssert.assertThat(
            PageTitle("Main Page", wiki).prefixedText,
            Matchers.`is`(wiki.titleForInternalLink("").prefixedText)
        )
        MatcherAssert.assertThat(
            PageTitle("Main Page", wiki).prefixedText,
            Matchers.`is`(wiki.titleForInternalLink("/wiki/").prefixedText)
        )
        MatcherAssert.assertThat(
            PageTitle("wiki", wiki).prefixedText,
            Matchers.`is`(wiki.titleForInternalLink("wiki").prefixedText)
        )
        MatcherAssert.assertThat(
            PageTitle("wiki", wiki).prefixedText,
            Matchers.`is`(wiki.titleForInternalLink("/wiki/wiki").prefixedText)
        )
        MatcherAssert.assertThat(
            PageTitle("wiki/wiki", wiki).prefixedText,
            Matchers.`is`(wiki.titleForInternalLink("/wiki/wiki/wiki").prefixedText)
        )
    }

    @Test
    fun testEquals() {
        MatcherAssert.assertThat(WikiSite.forLanguageCode("en"), Matchers.`is`(WikiSite.forLanguageCode("en")))
        MatcherAssert.assertThat(WikiSite.forLanguageCode("ta"), Matchers.not(WikiSite.forLanguageCode("en")))
        MatcherAssert.assertThat(
            WikiSite.forLanguageCode("ta").equals("ta.wikipedia.org"),
            Matchers.`is`(false)
        )
    }

    @Test
    fun testNormalization() {
        MatcherAssert.assertThat(
            "bm.wikipedia.org",
            Matchers.`is`(WikiSite.forLanguageCode("bm").authority())
        )
    }
}
