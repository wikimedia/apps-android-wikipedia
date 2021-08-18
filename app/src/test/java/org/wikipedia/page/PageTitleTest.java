package org.wikipedia.page;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.staticdata.MainPageNameData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@RunWith(RobolectricTestRunner.class) public class PageTitleTest {
    @Test public void testPrefixedText() {
        WikiSite enwiki = WikiSite.forLanguageCode("en");

        assertThat(new PageTitle(null, "Test  title",  enwiki).getPrefixedText(), is("Test__title"));
        assertThat(new PageTitle(null, "Test title",  enwiki).getPrefixedText(), is("Test_title"));
        assertThat(new PageTitle("Talk", "Test title",  enwiki).getPrefixedText(), is("Talk:Test_title"));
        assertThat(new PageTitle(null, "Test title",  enwiki).getText(), is("Test_title"));
    }

    @Test public void testFromInternalLink() {
        WikiSite enwiki = WikiSite.forLanguageCode("en");

        assertThat(enwiki.titleForInternalLink("/wiki/India").getPrefixedText(), is("India"));
        assertThat(enwiki.titleForInternalLink("/wiki/India").getNamespace(), emptyString());

        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India").getNamespace(), is("Talk"));
        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India").getText(), is("India"));
        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India").getFragment(), nullValue());

        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India#").getNamespace(), is("Talk"));
        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India#").getText(), is("India"));
        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India#").getFragment(), nullValue());

        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India#History").getNamespace(), is("Talk"));
        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India#History").getText(), is("India"));
        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India#History").getFragment(), is("History"));
    }

    @Test public void testCanonicalURL() {
        WikiSite enwiki = WikiSite.forLanguageCode("en");

        assertThat(enwiki.titleForInternalLink("/wiki/India").getUri(), is("https://en.wikipedia.org/wiki/India"));
        assertThat(enwiki.titleForInternalLink("/wiki/India Gate").getUri(), is("https://en.wikipedia.org/wiki/India_Gate"));
        assertThat(enwiki.titleForInternalLink("/wiki/India's Gate").getUri(), is("https://en.wikipedia.org/wiki/India%27s_Gate"));
    }

    @Test public void testVariants() {
        assertThat(new PageTitle("Taiwan", WikiSite.forLanguageCode("en")).getUri(), is("https://en.wikipedia.org/wiki/Taiwan"));
        assertThat(new PageTitle("Taiwan", WikiSite.forLanguageCode("zh")).getUri(), is("https://zh.wikipedia.org/zh/Taiwan"));
        assertThat(new PageTitle("Taiwan", WikiSite.forLanguageCode("zh-hant")).getUri(), is("https://zh.wikipedia.org/zh-hant/Taiwan"));
        assertThat(new PageTitle("Taiwan", WikiSite.forLanguageCode("zh-hans")).getUri(), is("https://zh.wikipedia.org/zh-hans/Taiwan"));
        assertThat(WikiSite.forLanguageCode("zh-hant").titleForInternalLink("/zh/Taiwan").getUri(), is("https://zh.wikipedia.org/zh-hant/Taiwan"));
        assertThat(WikiSite.forLanguageCode("zh-hant").titleForInternalLink("/zh-hant/Taiwan").getUri(), is("https://zh.wikipedia.org/zh-hant/Taiwan"));
        assertThat(WikiSite.forLanguageCode("zh-hant").titleForInternalLink("/wiki/Taiwan").getUri(), is("https://zh.wikipedia.org/zh-hant/Taiwan"));
    }

    @Test public void testWikiSite() {
        WikiSite enwiki = WikiSite.forLanguageCode("en");
        assertThat(new PageTitle(null, "Test", enwiki).getWikiSite(), is(enwiki));
        assertThat(WikiSite.forLanguageCode("en"), is(enwiki));
    }

    @Test public void testParsing() {
        WikiSite enwiki = WikiSite.forLanguageCode("en");

        assertThat(new PageTitle("Hello", enwiki).getDisplayText(), is("Hello"));
        assertThat(new PageTitle("Talk:Hello", enwiki).getDisplayText(), is("Talk:Hello"));
        assertThat(new PageTitle("Talk:Hello", enwiki).getText(), is("Hello"));
        assertThat(new PageTitle("Talk:Hello", enwiki).getNamespace(), is("Talk"));
        assertThat(new PageTitle("Wikipedia_talk:Hello world", enwiki).getDisplayText(), is("Wikipedia talk:Hello world"));
    }

    @Test public void testSpecial() {
        assertThat(new PageTitle("Special:Version", WikiSite.forLanguageCode("en")).isSpecial(), is(true));
        assertThat(new PageTitle("特別:Version", WikiSite.forLanguageCode("ja")).isSpecial(), is(true));
        assertThat(new PageTitle("Special:Version", WikiSite.forLanguageCode("ja")).isSpecial(), is(true));
        assertThat(new PageTitle("特別:Version", WikiSite.forLanguageCode("en")).isSpecial(), is(false));
    }

    @Test public void testFile() {
        assertThat(new PageTitle("File:SomethingSomething", WikiSite.forLanguageCode("en")).isFilePage(), is(true));
        assertThat(new PageTitle("ファイル:Version", WikiSite.forLanguageCode("ja")).isFilePage(), is(true));
        assertThat(new PageTitle("File:SomethingSomething", WikiSite.forLanguageCode("ja")).isFilePage(), is(true));
        assertThat(new PageTitle("ファイル:Version", WikiSite.forLanguageCode("en")).isFilePage(), is(false));
    }

    @Test public void testMainPage() {
        WikiSite enwiki = WikiSite.forLanguageCode("en");
        assertThat(new PageTitle("", enwiki).getPrefixedText(), is(new PageTitle(MainPageNameData.valueFor("en"), enwiki).getPrefixedText()));
    }

    @Test public void testIsMainPageNoTitleNoProps() {
        final String text = null;
        WikiSite wiki = WikiSite.forLanguageCode("test");
        PageTitle subject = new PageTitle(text, wiki);

        assertThat(subject.isMainPage(), is(true));
    }

    @Test public void testIsMainPageTitleNoProps() {
        String text = "text";
        WikiSite wiki = WikiSite.forLanguageCode("test");
        PageTitle subject = new PageTitle(text, wiki);

        assertThat(subject.isMainPage(), is(false));
    }

    /** https://bugzilla.wikimedia.org/66151 */
    @Test public void testHashChar() {
        PageTitle pageTitle = new PageTitle("#", WikiSite.forLanguageCode("en"));
        assertThat(pageTitle.getNamespace(), emptyString());
        assertThat(pageTitle.getText(), is(""));
        assertThat(pageTitle.getFragment(), nullValue());
    }

    @Test public void testColonChar() {
        PageTitle pageTitle = new PageTitle(":", WikiSite.forLanguageCode("en"));
        assertThat(pageTitle.getNamespace(), emptyString());
        assertThat(pageTitle.getText(), is(":"));
        assertThat(pageTitle.getFragment(), nullValue());
    }
}
