package org.wikipedia.page;

import org.wikipedia.Site;
import org.wikipedia.server.BasePageLeadTest;
import org.wikipedia.test.TestRunner;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/** Unit tests for Page. */
@RunWith(TestRunner.class)
public class PageTest {
    private static final Site SITE = new Site("en.wikipedia.org");

    private PageTitle title;
    private PageProperties props;

    @Before
    public void setUp() throws Exception {
        title = new PageTitle("Main page", SITE, "//foo/thumb.jpg");
        props = new PageProperties(new JSONObject(BasePageLeadTest.getEnglishMainPageJson()));
    }

    @Test
    public void testMediaWikiMarshalling() throws Exception {
        Page page = new Page(title, new ArrayList<Section>(), props, Page.MEDIAWIKI_ORIGIN);
        assertThat(page.isFromRestBase(), is(false));
        Page pageClone = new Page(page.toJSON());
        assertThat(pageClone.isFromRestBase(), is(false));
    }

    @Test
    public void testRestBaseMarshalling() throws Exception {
        Page page = new Page(title, new ArrayList<Section>(), props, Page.RESTBASE_ORIGIN);
        Page pageClone = new Page(page.toJSON()); // = complete unmarshall(marshall(subject))
        assertThat(pageClone, is(page));
        assertThat(page.isFromRestBase(), is(true));
        assertThat(pageClone.isFromRestBase(), is(true));
    }
}
