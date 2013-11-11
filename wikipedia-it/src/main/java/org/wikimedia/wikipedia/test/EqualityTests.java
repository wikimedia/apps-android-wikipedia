package org.wikimedia.wikipedia.test;

import junit.framework.TestCase;
import org.wikimedia.wikipedia.PageTitle;
import org.wikimedia.wikipedia.Site;

public class EqualityTests extends TestCase {
    public void testPageTitle() throws Exception {
        assertTrue(new PageTitle(null, "India", new Site("en.wikipedia.org")).equals(new PageTitle(null, "India", new Site("en.wikipedia.org"))));
        assertTrue(new PageTitle("Talk", "India", new Site("en.wikipedia.org")).equals(new PageTitle("Talk", "India", new Site("en.wikipedia.org"))));

        assertFalse(new PageTitle(null, "India", new Site("ta.wikipedia.org")).equals(new PageTitle(null, "India", new Site("en.wikipedia.org"))));
        assertFalse(new PageTitle("Talk", "India", new Site("ta.wikipedia.org")).equals(new PageTitle("Talk", "India", new Site("en.wikipedia.org"))));
        assertFalse(new PageTitle("Talk", "India", new Site("ta.wikipedia.org")).equals("Something else"));
    }

    public void testSite() throws Exception {
        assertTrue(new Site("en.wikipedia.org").equals(new Site("en.wikipedia.org")));

        assertFalse(new Site("ta.wikipedia.org").equals(new Site("en.wikipedia.org")));
        assertFalse(new Site("ta.wikipedia.org").equals("ta.wikipedia.org"));
    }
}
