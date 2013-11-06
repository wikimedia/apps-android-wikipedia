package org.wikimedia.wikipedia.test;

import junit.framework.TestCase;
import org.wikimedia.wikipedia.PageTitle;
import org.wikimedia.wikipedia.Site;

public class PageTitleTests extends TestCase {
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

    public void testSite() throws Exception {
        Site enwiki = new Site("en.wikipedia.org");

        assertEquals(enwiki, new PageTitle(null, "Test", enwiki).getSite());
        assertEquals(enwiki, new Site("en.wikipedia.org"));
    }
}
