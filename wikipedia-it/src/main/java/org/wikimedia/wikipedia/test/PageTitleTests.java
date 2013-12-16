package org.wikimedia.wikipedia.test;

import junit.framework.TestCase;
import org.wikimedia.wikipedia.PageTitle;
import org.wikimedia.wikipedia.Site;

public class PageTitleTests extends TestCase {

    public void testEquals() throws Exception {
        assertTrue(new PageTitle(null, "India", new Site("en.wikipedia.org")).equals(new PageTitle(null, "India", new Site("en.wikipedia.org"))));
        assertTrue(new PageTitle("Talk", "India", new Site("en.wikipedia.org")).equals(new PageTitle("Talk", "India", new Site("en.wikipedia.org"))));

        assertFalse(new PageTitle(null, "India", new Site("ta.wikipedia.org")).equals(new PageTitle(null, "India", new Site("en.wikipedia.org"))));
        assertFalse(new PageTitle("Talk", "India", new Site("ta.wikipedia.org")).equals(new PageTitle("Talk", "India", new Site("en.wikipedia.org"))));
        assertFalse(new PageTitle("Talk", "India", new Site("ta.wikipedia.org")).equals("Something else"));
    }

    public void testPrefixedText() throws Exception {
        Site enwiki = new Site("en.wikipedia.org");

        assertEquals(new PageTitle(null, "Test  title", enwiki).getPrefixedText(), "Test  title");
        assertEquals(new PageTitle(null, "Test title", enwiki).getPrefixedText(), "Test title");
        assertEquals(new PageTitle("Talk", "Test title", enwiki).getPrefixedText(), "Talk:Test title");
        assertEquals(new PageTitle(null, "Test   title", enwiki).getText(), "Test   title");
    }

    public void testFromInternalLink() throws Exception {
        Site enwiki = new Site("en.wikipedia.org");

        assertEquals(enwiki.titleForInternalLink("/wiki/India").getPrefixedText(), "India");
        assertNull(enwiki.titleForInternalLink("/wiki/India").getNamespace());

        assertEquals(enwiki.titleForInternalLink("/wiki/Talk:India").getNamespace(), "Talk");
        assertEquals(enwiki.titleForInternalLink("/wiki/Talk:India").getText(), "India");

        assertEquals(enwiki.titleForInternalLink("/wiki/Talk:India#History").getNamespace(), "Talk");
        assertEquals(enwiki.titleForInternalLink("/wiki/Talk:India#History").getText(), "India");
    }

    public void testCanonicalURL() throws Exception {
        Site enwiki = new Site("en.wikipedia.org");

        assertEquals(enwiki.titleForInternalLink("/wiki/India").getCanonicalUri(), "https://en.wikipedia.org/wiki/India");
        assertEquals(enwiki.titleForInternalLink("/wiki/India Gate").getCanonicalUri(), "https://en.wikipedia.org/wiki/India_Gate");
        assertEquals(enwiki.titleForInternalLink("/wiki/India's Gate").getCanonicalUri(), "https://en.wikipedia.org/wiki/India%27s_Gate");
    }

    public void testSite() throws Exception {
        Site enwiki = new Site("en.wikipedia.org");

        assertEquals(enwiki, new PageTitle(null, "Test", enwiki).getSite());
        assertEquals(enwiki, new Site("en.wikipedia.org"));
    }
}
