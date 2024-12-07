package org.wikipedia.page

import android.net.Uri
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
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

        MatcherAssert.assertThat(PageTitle(null, "Test  title", enwiki).prefixedText,
            Matchers.`is`("Test__title"))
        MatcherAssert.assertThat(PageTitle(null, "Test title", enwiki).prefixedText,
            Matchers.`is`("Test_title"))
        MatcherAssert.assertThat(PageTitle("Talk", "Test title", enwiki).prefixedText,
            Matchers.`is`("Talk:Test_title"))
        MatcherAssert.assertThat(PageTitle(null, "Test title", enwiki).text,
            Matchers.`is`("Test_title"))
    }

    @Test
    fun testFromInternalLink() {
        val enwiki = WikiSite.forLanguageCode("en")

        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/wiki/India", enwiki).prefixedText,
            Matchers.`is`("India"))
        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/wiki/India", enwiki).namespace,
            Matchers.emptyString())
        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/wiki/Talk:India", enwiki).namespace,
            Matchers.`is`("Talk"))
        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/wiki/Talk:India", enwiki).text,
            Matchers.`is`("India"))
        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/wiki/Talk:India", enwiki).fragment,
            Matchers.nullValue())
        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/wiki/Talk:India#", enwiki).namespace,
            Matchers.`is`("Talk"))
        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/wiki/Talk:India#", enwiki).text,
            Matchers.`is`("India"))
        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/wiki/Talk:India#", enwiki).fragment,
            Matchers.nullValue())
        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/wiki/Talk:India#History", enwiki).namespace,
            Matchers.`is`("Talk"))
        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/wiki/Talk:India#History", enwiki).text,
            Matchers.`is`("India"))
        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/wiki/Talk:India#History", enwiki).fragment,
            Matchers.`is`("History"))
    }

    @Test
    fun testCanonicalURL() {
        val enwiki = WikiSite.forLanguageCode("en")

        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/wiki/India", enwiki).uri,
            Matchers.`is`("https://en.wikipedia.org/wiki/India"))
        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/wiki/India Gate", enwiki).uri,
            Matchers.`is`("https://en.wikipedia.org/wiki/India_Gate"))
        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/wiki/India's Gate", enwiki).uri,
            Matchers.`is`("https://en.wikipedia.org/wiki/India%27s_Gate"))
    }

    @Test
    fun testVariants() {
        val zhwiki = WikiSite.forLanguageCode("zh-tw")

        MatcherAssert.assertThat(PageTitle("Taiwan", WikiSite.forLanguageCode("en")).uri,
            Matchers.`is`("https://en.wikipedia.org/wiki/Taiwan"))
        MatcherAssert.assertThat(PageTitle("Taiwan", WikiSite.forLanguageCode("zh")).uri,
            Matchers.`is`("https://zh.wikipedia.org/zh/Taiwan"))
        MatcherAssert.assertThat(PageTitle("Taiwan", WikiSite.forLanguageCode("zh-tw")).uri,
            Matchers.`is`("https://zh.wikipedia.org/zh-tw/Taiwan"))
        MatcherAssert.assertThat(PageTitle("Taiwan", WikiSite.forLanguageCode("zh-cn")).uri,
            Matchers.`is`("https://zh.wikipedia.org/zh-cn/Taiwan"))
        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/zh/Taiwan", zhwiki).uri,
            Matchers.`is`("https://zh.wikipedia.org/zh-tw/Taiwan"))
        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/zh-tw/Taiwan", zhwiki).uri,
            Matchers.`is`("https://zh.wikipedia.org/zh-tw/Taiwan"))
        MatcherAssert.assertThat(PageTitle.titleForInternalLink("/wiki/Taiwan", zhwiki).uri,
            Matchers.`is`("https://zh.wikipedia.org/zh-tw/Taiwan"))
    }

    @Test
    fun testWikiSite() {
        val enwiki = WikiSite.forLanguageCode("en")
        MatcherAssert.assertThat(PageTitle(null, "Test", enwiki).wikiSite, Matchers.`is`(enwiki))
        MatcherAssert.assertThat(WikiSite.forLanguageCode("en"), Matchers.`is`(enwiki))
    }

    @Test
    fun testLangAsNamespace() {
        val uri = Uri.parse("https://en.wikipedia.org/wiki/fr:Article")
        val site = WikiSite(uri)
        val title = PageTitle.titleForUri(uri, site)
        MatcherAssert.assertThat(title.wikiSite.authority(), Matchers.`is`("fr.wikipedia.org"))
        MatcherAssert.assertThat(title.displayText, Matchers.`is`("Article"))
    }

    @Test
    fun testParsing() {
        val enwiki = WikiSite.forLanguageCode("en")

        MatcherAssert.assertThat(PageTitle("Hello", enwiki).displayText, Matchers.`is`("Hello"))
        MatcherAssert.assertThat(PageTitle("Talk:Hello", enwiki).displayText,
            Matchers.`is`("Talk:Hello"))
        MatcherAssert.assertThat(PageTitle("Talk:Hello", enwiki).text, Matchers.`is`("Hello"))
        MatcherAssert.assertThat(PageTitle("Talk:Hello", enwiki).namespace, Matchers.`is`("Talk"))
        MatcherAssert.assertThat(PageTitle("Wikipedia_talk:Hello world", enwiki).displayText,
            Matchers.`is`("Wikipedia talk:Hello world"))
    }

    @Test
    fun testSpecial() {
        MatcherAssert.assertThat(PageTitle("Special:Version", WikiSite.forLanguageCode("en")).isSpecial,
            Matchers.`is`(true))
        MatcherAssert.assertThat(PageTitle("特別:Version", WikiSite.forLanguageCode("ja")).isSpecial,
            Matchers.`is`(true))
        MatcherAssert.assertThat(PageTitle("Special:Version", WikiSite.forLanguageCode("ja")).isSpecial,
            Matchers.`is`(true))
        MatcherAssert.assertThat(PageTitle("特別:Version", WikiSite.forLanguageCode("en")).isSpecial,
            Matchers.`is`(false))
    }

    @Test
    fun testFile() {
        MatcherAssert.assertThat(PageTitle("File:SomethingSomething", WikiSite.forLanguageCode("en")).isFilePage,
            Matchers.`is`(true))
        MatcherAssert.assertThat(PageTitle("ファイル:Version", WikiSite.forLanguageCode("ja")).isFilePage,
            Matchers.`is`(true))
        MatcherAssert.assertThat(PageTitle("File:SomethingSomething", WikiSite.forLanguageCode("ja")).isFilePage,
            Matchers.`is`(true))
        MatcherAssert.assertThat(PageTitle("ファイル:Version", WikiSite.forLanguageCode("en")).isFilePage,
            Matchers.`is`(false))
    }

    @Test
    fun testMainPage() {
        val enwiki = WikiSite.forLanguageCode("en")
        MatcherAssert.assertThat(PageTitle("", enwiki).prefixedText,
            Matchers.`is`(PageTitle(MainPageNameData.valueFor("en"), enwiki).prefixedText))
    }

    @Test
    fun testIsMainPageNoTitleNoProps() {
        val text: String? = null
        val wiki = WikiSite.forLanguageCode("test")
        val subject = PageTitle(text, wiki)

        MatcherAssert.assertThat(subject.isMainPage, Matchers.`is`(true))
    }

    @Test
    fun testIsMainPageTitleNoProps() {
        val text = "text"
        val wiki = WikiSite.forLanguageCode("test")
        val subject = PageTitle(text, wiki)

        MatcherAssert.assertThat(subject.isMainPage, Matchers.`is`(false))
    }

    /** https://bugzilla.wikimedia.org/66151  */
    @Test
    fun testHashChar() {
        val pageTitle = PageTitle("#", WikiSite.forLanguageCode("en"))
        MatcherAssert.assertThat(pageTitle.namespace, Matchers.emptyString())
        MatcherAssert.assertThat(pageTitle.text, Matchers.`is`(""))
        MatcherAssert.assertThat(pageTitle.fragment, Matchers.nullValue())
    }

    @Test
    fun testColonChar() {
        val pageTitle = PageTitle(":", WikiSite.forLanguageCode("en"))
        MatcherAssert.assertThat(pageTitle.namespace, Matchers.emptyString())
        MatcherAssert.assertThat(pageTitle.text, Matchers.`is`(":"))
        MatcherAssert.assertThat(pageTitle.fragment, Matchers.nullValue())
    }

    @Test
    fun testEquality() {
        val title1 = PageTitle("Test Article", WikiSite.forLanguageCode("en"))
        var title2 = PageTitle("Test Article", WikiSite.forLanguageCode("en"))
        MatcherAssert.assertThat(title1, Matchers.`is`(title2))
        MatcherAssert.assertThat(title2, Matchers.`is`(title1))
        title2.displayText = "TEST ARTICLE"
        title2.thumbUrl = "http://thumb.url"
        title2.extract = "Foo"
        title2.description = "Bar"
        MatcherAssert.assertThat(title1, Matchers.`is`(title2))
        MatcherAssert.assertThat(title2, Matchers.`is`(title1))
        title2.namespace = "File"
        MatcherAssert.assertThat(title1, Matchers.not(title2))
        MatcherAssert.assertThat(title2, Matchers.not(title1))

        title2 = PageTitle("Test Article", WikiSite.forLanguageCode("ru"))
        MatcherAssert.assertThat(title1, Matchers.not(title2))
        MatcherAssert.assertThat(title2, Matchers.not(title1))

        MatcherAssert.assertThat(title1, Matchers.not(Any()))
    }
}
