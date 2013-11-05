package org.wikimedia.wikipedia.test;

import junit.framework.TestCase;
import org.wikimedia.wikipedia.PageTitle;

public class PageTitleTests extends TestCase {
    public void testPrefixedText() throws Exception {
        assertEquals(new PageTitle(null, "Test  title").getPrefixedText(), "Test  title");
        assertEquals(new PageTitle(null, "Test title").getPrefixedText(), "Test title");
        assertEquals(new PageTitle("Talk", "Test title").getPrefixedText(), "Talk:Test title");
        assertEquals(new PageTitle(null, "Test   title").getText(), "Test   title");
    }

    public void testFromInternalLink() throws Exception {
        assertEquals(PageTitle.fromInternalLink("/wiki/India").getPrefixedText(), "India");
        assertNull(PageTitle.fromInternalLink("/wiki/India").getNamespace());

        assertEquals(PageTitle.fromInternalLink("/wiki/Talk:India").getNamespace(), "Talk");
        assertEquals(PageTitle.fromInternalLink("/wiki/Talk:India").getText(), "India");

        assertEquals(PageTitle.fromInternalLink("/wiki/Talk:India#History").getNamespace(), "Talk");
        assertEquals(PageTitle.fromInternalLink("/wiki/Talk:India#History").getText(), "India");
    }
}
