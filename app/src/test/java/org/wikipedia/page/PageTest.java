package org.wikipedia.page;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.server.BasePageLeadTest;
import org.wikipedia.test.TestRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/** Unit tests for Page. */
@RunWith(TestRunner.class)
public class PageTest {
    private static final WikiSite WIKI = WikiSite.forLanguageCode("en");

    @Test
    public void testMediaWikiMarshalling() throws Throwable {
        PageTitle title = new PageTitle("Main page", WIKI, "//foo/thumb.jpg");
        PageProperties props = new PageProperties(new JSONObject(BasePageLeadTest.getEnglishMainPageJson()));

        Page page = new Page(title, new ArrayList<Section>(), props, Page.MEDIAWIKI_ORIGIN);
        assertThat(page.isFromRestBase(), is(false));
        Page pageClone = new Page(page.toJSON());
        assertThat(pageClone.isFromRestBase(), is(false));
    }

    @Test
    public void testRestBaseMarshalling() throws Throwable {
        PageTitle title = new PageTitle("Main page", WIKI, "//foo/thumb.jpg");
        PageProperties props = new PageProperties(new JSONObject(BasePageLeadTest.getEnglishMainPageJson()));

        Page page = new Page(title, new ArrayList<Section>(), props, Page.RESTBASE_ORIGIN);
        Page pageClone = new Page(page.toJSON()); // = complete unmarshall(marshall(subject))
        assertThat(pageClone, is(page));
        assertThat(page.isFromRestBase(), is(true));
        assertThat(pageClone.isFromRestBase(), is(true));
    }

    @Test public void testConstructorJson() throws Exception {
        List<Section> sections = new ArrayList<>();
        Section headSection = new Section(0, 1, null, null, "Hi there!");
        sections.add(headSection);
        final int numSections = 10;
        for (int i = 1; i <= numSections; i++) {
            sections.add(new Section(i, 1, "Something " + i, "Something_" + i, "Content Something" + i));
        }
        PageTitle title = new PageTitle(null, "Test", WikiSite.forLanguageCode("en"));
        PageProperties props = new PageProperties(new JSONObject("{\"id\":15580374,\"displaytitle\":\"Test\",\"revision\":615503846,\"lastmodified\":\"2001-02-03T04:00:00Z\",\"editable\":true,\"mainpage\":true}"));
        Page page = new Page(title, sections, props);
        assertThat(page, is(new Page(page.toJSON())));
    }
}