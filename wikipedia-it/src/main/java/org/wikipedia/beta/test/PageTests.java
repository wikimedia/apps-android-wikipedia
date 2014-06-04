package org.wikipedia.beta.test;

import junit.framework.TestCase;
import org.wikipedia.beta.PageTitle;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.page.Page;
import org.wikipedia.beta.page.PageProperties;
import org.wikipedia.beta.page.Section;

import java.util.ArrayList;

public class PageTests extends TestCase {

    public void testJSONSerialization() throws Exception {
        ArrayList<Section> sections = new ArrayList<Section>();
        Section headSection = new Section(0, 1, null, null, "Hi there!");
        sections.add(headSection);
        for (int i = 1; i <= 10; i++) {
            sections.add(new Section(i, 1, "Something " + i, "Something_" + i, "Content Something" + i));
        }
        PageTitle title = new PageTitle(null, "Test", new Site("en.wikipedia.org"));
        PageProperties props = new PageProperties("2001-02-03T04:00:00Z", "Test", null, true, true);
        Page page = new Page(title, sections, props);
        assertEquals(page, new Page(page.toJSON()));
    }
}
