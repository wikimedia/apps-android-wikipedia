package org.wikipedia.test;

import junit.framework.TestCase;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;

public class SiteTests extends TestCase {

    public void testEquals() throws Exception {
        assertTrue(new Site("en.wikipedia.org").equals(new Site("en.wikipedia.org")));

        assertFalse(new Site("ta.wikipedia.org").equals(new Site("en.wikipedia.org")));
        assertFalse(new Site("ta.wikipedia.org").equals("ta.wikipedia.org"));
    }

    public void testNormalization() throws Exception {
        assertEquals(new Site("en.wikipedia.org"), new Site("en.m.wikipedia.org"));
        assertEquals("bm.wikipedia.org", new Site("bm.wikipedia.org").authority());
    }

    public void testIsSupportedSite() throws Exception {
        assertTrue(Site.supportedAuthority("fr.wikipedia.org"));
        assertTrue(Site.supportedAuthority("fr.m.wikipedia.org"));
        assertTrue(Site.supportedAuthority("roa-rup.wikipedia.org"));

        assertFalse(Site.supportedAuthority("google.com"));
    }

    public void testTitleForInternalLink() {
        Site site = new Site("en.wikipedia.org");
        assertEquals(new PageTitle("Main Page", site), site.titleForInternalLink(""));
        assertEquals(new PageTitle("Main Page", site), site.titleForInternalLink("/wiki/"));
        assertEquals(new PageTitle("wiki", site), site.titleForInternalLink("wiki"));
        assertEquals(new PageTitle("wiki", site), site.titleForInternalLink("/wiki/wiki"));
        assertEquals(new PageTitle("wiki/wiki", site), site.titleForInternalLink("/wiki/wiki/wiki"));
    }
}
