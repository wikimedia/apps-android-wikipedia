package org.wikipedia.test;

import android.support.test.filters.SmallTest;

import junit.framework.TestCase;

import org.json.JSONObject;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.Section;

import java.util.ArrayList;
import java.util.List;

@SmallTest
public class PageTests extends TestCase {

    private static final int NUM_SECTIONS = 10;

    public void testJSONSerialization() throws Exception {
        List<Section> sections = new ArrayList<>();
        Section headSection = new Section(0, 1, null, null, "Hi there!");
        sections.add(headSection);
        for (int i = 1; i <= NUM_SECTIONS; i++) {
            sections.add(new Section(i, 1, "Something " + i, "Something_" + i, "Content Something" + i));
        }
        PageTitle title = new PageTitle(null, "Test", WikiSite.forLanguageCode("en"));
        PageProperties props = new PageProperties(new JSONObject("{\"id\":15580374,\"displaytitle\":\"Test\",\"revision\":615503846,\"lastmodified\":\"2001-02-03T04:00:00Z\",\"editable\":true,\"mainpage\":true}"));
        Page page = new Page(title, sections, props);
        assertEquals(page, new Page(page.toJSON()));
    }
}
