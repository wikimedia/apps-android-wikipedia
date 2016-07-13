package org.wikipedia.page;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.Site;
import org.wikipedia.staticdata.MainPageNameData;
import org.wikipedia.test.TestRunner;

@RunWith(TestRunner.class)
public class PageTitleTest extends TestCase {

    @Test public void testEquals() throws Exception {
        assertTrue(new PageTitle(null, "India", new Site("en.wikipedia.org")).equals(new PageTitle(null, "India", new Site("en.wikipedia.org"))));
        assertTrue(new PageTitle("Talk", "India",  new Site("en.wikipedia.org")).equals(new PageTitle("Talk", "India", new Site("en.wikipedia.org"))));

        assertFalse(new PageTitle(null, "India",  new Site("ta.wikipedia.org")).equals(new PageTitle(null, "India", new Site("en.wikipedia.org"))));
        assertFalse(new PageTitle("Talk", "India",  new Site("ta.wikipedia.org")).equals(new PageTitle("Talk", "India", new Site("en.wikipedia.org"))));
        assertFalse(new PageTitle("Talk", "India",  new Site("ta.wikipedia.org")).equals("Something else"));
    }

    @Test public void testJSONSerialization() throws Exception {
        Site enwiki = new Site("en.wikipedia.org");
        PageTitle title = new PageTitle(null, "Test title",  enwiki);
        assertEquals(title, new PageTitle(title.toJSON()));

        title = new PageTitle("Talk", "Test title",  enwiki);
        assertEquals(title, new PageTitle(title.toJSON()));
    }

    @Test public void testPrefixedText() throws Exception {
        Site enwiki = new Site("en.wikipedia.org");

        assertEquals(new PageTitle(null, "Test  title",  enwiki).getPrefixedText(), "Test__title");
        assertEquals(new PageTitle(null, "Test title",  enwiki).getPrefixedText(), "Test_title");
        assertEquals(new PageTitle("Talk", "Test title",  enwiki).getPrefixedText(), "Talk:Test_title");
        assertEquals(new PageTitle(null, "Test title",  enwiki).getText(), "Test_title");
    }

    @Test public void testFromInternalLink() throws Exception {
        Site enwiki = new Site("en.wikipedia.org");

        assertEquals(enwiki.titleForInternalLink("/wiki/India").getPrefixedText(), "India");
        assertNull(enwiki.titleForInternalLink("/wiki/India").getNamespace());

        assertEquals(enwiki.titleForInternalLink("/wiki/Talk:India").getNamespace(), "Talk");
        assertEquals(enwiki.titleForInternalLink("/wiki/Talk:India").getText(), "India");
        assertEquals(enwiki.titleForInternalLink("/wiki/Talk:India").getFragment(), null);

        assertEquals(enwiki.titleForInternalLink("/wiki/Talk:India#").getNamespace(), "Talk");
        assertEquals(enwiki.titleForInternalLink("/wiki/Talk:India#").getText(), "India");
        assertEquals(enwiki.titleForInternalLink("/wiki/Talk:India#").getFragment(), "");

        assertEquals(enwiki.titleForInternalLink("/wiki/Talk:India#History").getNamespace(), "Talk");
        assertEquals(enwiki.titleForInternalLink("/wiki/Talk:India#History").getText(), "India");
        assertEquals(enwiki.titleForInternalLink("/wiki/Talk:India#History").getFragment(), "History");
    }

    @Test public void testCanonicalURL() throws Exception {
        Site enwiki = new Site("en.wikipedia.org");

        assertEquals(enwiki.titleForInternalLink("/wiki/India").getCanonicalUri(), "https://en.wikipedia.org/wiki/India");
        assertEquals(enwiki.titleForInternalLink("/wiki/India Gate").getCanonicalUri(), "https://en.wikipedia.org/wiki/India_Gate");
        assertEquals(enwiki.titleForInternalLink("/wiki/India's Gate").getCanonicalUri(), "https://en.wikipedia.org/wiki/India%27s_Gate");
    }

    @Test public void testSite() throws Exception {
        Site enwiki = new Site("en.wikipedia.org");

        assertEquals(enwiki, new PageTitle(null, "Test", enwiki).getSite());
        assertEquals(enwiki, new Site("en.wikipedia.org"));
    }

    @Test public void testParsing() throws Exception {
        Site enwiki = new Site("en.wikipedia.org");

        assertEquals("Hello", new PageTitle("Hello", enwiki).getDisplayText());
        assertEquals("Talk:Hello", new PageTitle("Talk:Hello", enwiki).getDisplayText());
        assertEquals("Hello", new PageTitle("Talk:Hello", enwiki).getText());
        assertEquals("Talk", new PageTitle("Talk:Hello", enwiki).getNamespace());
        assertEquals("Wikipedia talk:Hello world", new PageTitle("Wikipedia_talk:Hello world", enwiki).getDisplayText());
    }

    @Test public void testSpecial() throws Exception {
        assertTrue(new PageTitle("Special:Version", new Site("en.wikipedia.org")).isSpecial());
        assertTrue(new PageTitle("特別:Version", new Site("ja.wikipedia.org")).isSpecial());
        assertFalse(new PageTitle("Special:Version", new Site("ja.wikipedia.org")).isSpecial());
        assertFalse(new PageTitle("特別:Version", new Site("en.wikipedia.org")).isSpecial());
    }

    @Test public void testFile() throws Exception {
        assertTrue(new PageTitle("File:SomethingSomething", new Site("en.wikipedia.org")).isFilePage());
        assertTrue(new PageTitle("ファイル:Version", new Site("ja.wikipedia.org")).isFilePage());
        assertFalse(new PageTitle("File:SomethingSomething", new Site("ja.wikipedia.org")).isFilePage());
        assertFalse(new PageTitle("ファイル:Version", new Site("en.wikipedia.org")).isFilePage());
    }

    @Test public void testMainpage() throws Exception {
        Site enwiki = new Site("en.wikipedia.org");
        assertEquals(new PageTitle("", enwiki), new PageTitle(MainPageNameData.valueFor("en"), enwiki));
    }

    /** https://bugzilla.wikimedia.org/66151 */
    @Test public void testHashChar() {
        PageTitle pageTitle = new PageTitle("#", new Site("en.wikipedia.org"));
        assertEquals(null, pageTitle.getNamespace());
        assertEquals("", pageTitle.getText());
        assertEquals("", pageTitle.getFragment());
    }

    @Test public void testColonChar() {
        PageTitle pageTitle = new PageTitle(":", new Site("en.wikipedia.org"));
        assertEquals("", pageTitle.getNamespace());
        assertEquals("", pageTitle.getText());
        assertEquals(null, pageTitle.getFragment());
    }
}
