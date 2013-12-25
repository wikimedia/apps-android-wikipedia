package org.wikimedia.wikipedia.test;

import junit.framework.TestCase;
import org.wikimedia.wikipedia.*;

import java.util.ArrayList;
import java.util.Date;

public class PageTests extends TestCase {

    public void testJSONSerialization() throws Exception {
        ArrayList<Section> sections = new ArrayList<Section>();
        Section headSection = new Section(0, 1, null, null, "Hi there!");
        sections.add(headSection);
        for (int i = 1; i <= 10; i++) {
            sections.add(new Section(i, 1, "Something " + i, "Something_" + i, "Content Something" + i ));
        }
        PageTitle title = new PageTitle(null, "Test", new Site("en.wikipedia.org"));
        PageProperties props = new PageProperties(new Date());
        Page page = new Page(title, sections, props);
        assertEquals(page, new Page(page.toJSON()));
    }
}
