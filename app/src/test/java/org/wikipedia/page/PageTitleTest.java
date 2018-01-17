package org.wikipedia.page;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.staticdata.MainPageNameData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class) public class PageTitleTest {
    @Test public void testEquals() throws Throwable {
        assertThat(new PageTitle(null, "India", WikiSite.forLanguageCode("en")).equals(new PageTitle(null, "India", WikiSite.forLanguageCode("en"))), is(true));
        assertThat(new PageTitle("Talk", "India",  WikiSite.forLanguageCode("en")).equals(new PageTitle("Talk", "India", WikiSite.forLanguageCode("en"))), is(true));

        assertThat(new PageTitle(null, "India",  WikiSite.forLanguageCode("ta")).equals(new PageTitle(null, "India", WikiSite.forLanguageCode("en"))), is(false));
        assertThat(new PageTitle("Talk", "India",  WikiSite.forLanguageCode("ta")).equals(new PageTitle("Talk", "India", WikiSite.forLanguageCode("en"))), is(false));
        assertThat(new PageTitle("Talk", "India",  WikiSite.forLanguageCode("ta")).equals("Something else"), is(false));
    }

    @Test public void testJsonSerialization() throws Throwable {
        WikiSite enwiki = WikiSite.forLanguageCode("en");
        PageTitle title = new PageTitle(null, "Test title",  enwiki);
        assertThat(title, is(new PageTitle(title.toJSON())));

        title = new PageTitle("Talk", "Test title",  enwiki);
        assertThat(new PageTitle(title.toJSON()), is(title));
    }

    @Test public void testPrefixedText() throws Throwable {
        WikiSite enwiki = WikiSite.forLanguageCode("en");

        assertThat(new PageTitle(null, "Test  title",  enwiki).getPrefixedText(), is("Test__title"));
        assertThat(new PageTitle(null, "Test title",  enwiki).getPrefixedText(), is("Test_title"));
        assertThat(new PageTitle("Talk", "Test title",  enwiki).getPrefixedText(), is("Talk:Test_title"));
        assertThat(new PageTitle(null, "Test title",  enwiki).getText(), is("Test_title"));
    }

    @Test public void testFromInternalLink() throws Throwable {
        WikiSite enwiki = WikiSite.forLanguageCode("en");

        assertThat(enwiki.titleForInternalLink("/wiki/India").getPrefixedText(), is("India"));
        assertThat(enwiki.titleForInternalLink("/wiki/India").getNamespace(), nullValue());

        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India").getNamespace(), is("Talk"));
        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India").getText(), is("India"));
        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India").getFragment(), nullValue());

        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India#").getNamespace(), is("Talk"));
        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India#").getText(), is("India"));
        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India#").getFragment(), is(""));

        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India#History").getNamespace(), is("Talk"));
        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India#History").getText(), is("India"));
        assertThat(enwiki.titleForInternalLink("/wiki/Talk:India#History").getFragment(), is("History"));
    }

    @Test public void testCanonicalURL() throws Throwable {
        WikiSite enwiki = WikiSite.forLanguageCode("en");

        assertThat(enwiki.titleForInternalLink("/wiki/India").getCanonicalUri(), is("https://en.wikipedia.org/wiki/India"));
        assertThat(enwiki.titleForInternalLink("/wiki/India Gate").getCanonicalUri(), is("https://en.wikipedia.org/wiki/India_Gate"));
        assertThat(enwiki.titleForInternalLink("/wiki/India's Gate").getCanonicalUri(), is("https://en.wikipedia.org/wiki/India%27s_Gate"));
    }

    @Test public void testWikiSite() throws Throwable {
        WikiSite enwiki = WikiSite.forLanguageCode("en");

        assertThat(new PageTitle(null, "Test", enwiki).getWikiSite(), is(enwiki));
        assertThat(WikiSite.forLanguageCode("en"), is(enwiki));
    }

    @Test public void testParsing() throws Throwable {
        WikiSite enwiki = WikiSite.forLanguageCode("en");

        assertThat(new PageTitle("Hello", enwiki).getDisplayText(), is("Hello"));
        assertThat(new PageTitle("Talk:Hello", enwiki).getDisplayText(), is("Talk:Hello"));
        assertThat(new PageTitle("Talk:Hello", enwiki).getText(), is("Hello"));
        assertThat(new PageTitle("Talk:Hello", enwiki).getNamespace(), is("Talk"));
        assertThat(new PageTitle("Wikipedia_talk:Hello world", enwiki).getDisplayText(), is("Wikipedia talk:Hello world"));
    }

    @Test public void testSpecial() throws Throwable {
        assertThat(new PageTitle("Special:Version", WikiSite.forLanguageCode("en")).isSpecial(), is(true));
        assertThat(new PageTitle("特別:Version", WikiSite.forLanguageCode("ja")).isSpecial(), is(true));
        assertThat(new PageTitle("Special:Version", WikiSite.forLanguageCode("ja")).isSpecial(), is(false));
        assertThat(new PageTitle("特別:Version", WikiSite.forLanguageCode("en")).isSpecial(), is(false));
    }

    @Test public void testFile() throws Throwable {
        assertThat(new PageTitle("File:SomethingSomething", WikiSite.forLanguageCode("en")).isFilePage(), is(true));
        assertThat(new PageTitle("ファイル:Version", WikiSite.forLanguageCode("ja")).isFilePage(), is(true));
        assertThat(new PageTitle("File:SomethingSomething", WikiSite.forLanguageCode("ja")).isFilePage(), is(false));
        assertThat(new PageTitle("ファイル:Version", WikiSite.forLanguageCode("en")).isFilePage(), is(false));
    }

    @Test public void testMainPage() throws Throwable {
        WikiSite enwiki = WikiSite.forLanguageCode("en");
        assertThat(new PageTitle("", enwiki), is(new PageTitle(MainPageNameData.valueFor("en"), enwiki)));
    }

    @Test public void testIsMainPageNoTitleNoProps() throws Throwable {
        final String text = null;
        WikiSite wiki = WikiSite.forLanguageCode("test");
        final String thumbUrl = null;
        final String desc = null;
        final PageProperties props = null;
        PageTitle subject = new PageTitle(text, wiki, thumbUrl, desc, props);

        assertThat(subject.isMainPage(), is(true));
    }

    @Test public void testIsMainPageTitleNoProps() throws Throwable {
        String text = "text";
        WikiSite wiki = WikiSite.forLanguageCode("test");
        final String thumbUrl = null;
        final String desc = null;
        final PageProperties props = null;
        PageTitle subject = new PageTitle(text, wiki, thumbUrl, desc, props);

        assertThat(subject.isMainPage(), is(false));
    }

    @Test public void testIsMainPageProps() throws Throwable {
        String text = "text";
        WikiSite wiki = WikiSite.forLanguageCode("test");
        final String thumbUrl = null;
        final String desc = null;
        PageProperties props = mock(PageProperties.class);
        when(props.isMainPage()).thenReturn(true);
        PageTitle subject = new PageTitle(text, wiki, thumbUrl, desc, props);

        assertThat(subject.isMainPage(), is(true));
    }

    /** https://bugzilla.wikimedia.org/66151 */
    @Test public void testHashChar() {
        PageTitle pageTitle = new PageTitle("#", WikiSite.forLanguageCode("en"));
        assertThat(pageTitle.getNamespace(), nullValue());
        assertThat(pageTitle.getText(), is(""));
        assertThat(pageTitle.getFragment(), is(""));
    }

    @Test public void testColonChar() {
        PageTitle pageTitle = new PageTitle(":", WikiSite.forLanguageCode("en"));
        assertThat(pageTitle.getNamespace(), is(""));
        assertThat(pageTitle.getText(), is(""));
        assertThat(pageTitle.getFragment(), nullValue());
    }
}
