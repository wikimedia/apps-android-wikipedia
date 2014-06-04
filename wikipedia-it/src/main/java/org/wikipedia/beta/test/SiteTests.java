package org.wikipedia.beta.test;

import junit.framework.TestCase;
import org.wikipedia.beta.Site;

public class SiteTests extends TestCase {

    public void testEquals() throws Exception {
        assertTrue(new Site("en.wikipedia.org").equals(new Site("en.wikipedia.org")));

        assertFalse(new Site("ta.wikipedia.org").equals(new Site("en.wikipedia.org")));
        assertFalse(new Site("ta.wikipedia.org").equals("ta.wikipedia.org"));
    }

    public void testNormalization() throws Exception {
        assertEquals(new Site("en.wikipedia.org"), new Site("en.m.wikipedia.org"));
        assertEquals("bm.wikipedia.org", (new Site("bm.wikipedia.org")).getDomain());
    }

    public void testIsSupportedSite() throws Exception {
        assertTrue(Site.isSupportedSite("fr.wikipedia.org"));
        assertTrue(Site.isSupportedSite("fr.m.wikipedia.org"));
        assertTrue(Site.isSupportedSite("roa-rup.wikipedia.org"));

        assertFalse(Site.isSupportedSite("google.com"));
    }
}
