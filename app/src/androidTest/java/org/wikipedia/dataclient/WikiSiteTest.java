package org.wikipedia.dataclient;

import android.support.test.filters.SmallTest;

import junit.framework.TestCase;

import org.wikipedia.page.PageTitle;

@SmallTest
public class WikiSiteTest extends TestCase {

    public void testEquals() throws Exception {
        assertTrue(WikiSite.forLanguageCode("en").equals(WikiSite.forLanguageCode("en")));

        assertFalse(WikiSite.forLanguageCode("ta").equals(WikiSite.forLanguageCode("en")));
        assertFalse(WikiSite.forLanguageCode("ta").equals("ta.wikipedia.org"));
    }

    public void testNormalization() throws Exception {
        assertEquals(WikiSite.forLanguageCode("en"), new WikiSite("en.m.wikipedia.org"));
        assertEquals("bm.wikipedia.org", WikiSite.forLanguageCode("bm").authority());
    }

    public void testIsSupportedSite() throws Exception {
        assertTrue(WikiSite.supportedAuthority("fr.wikipedia.org"));
        assertTrue(WikiSite.supportedAuthority("fr.m.wikipedia.org"));
        assertTrue(WikiSite.supportedAuthority("roa-rup.wikipedia.org"));

        assertFalse(WikiSite.supportedAuthority("google.com"));
    }

    public void testTitleForInternalLink() {
        WikiSite wiki = WikiSite.forLanguageCode("en");
        assertEquals(new PageTitle("Main Page", wiki), wiki.titleForInternalLink(""));
        assertEquals(new PageTitle("Main Page", wiki), wiki.titleForInternalLink("/wiki/"));
        assertEquals(new PageTitle("wiki", wiki), wiki.titleForInternalLink("wiki"));
        assertEquals(new PageTitle("wiki", wiki), wiki.titleForInternalLink("/wiki/wiki"));
        assertEquals(new PageTitle("wiki/wiki", wiki), wiki.titleForInternalLink("/wiki/wiki/wiki"));
    }
}
