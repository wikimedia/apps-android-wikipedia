package org.wikipedia.page;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.BasePageLeadTest;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/** Unit tests for Page. */
@RunWith(RobolectricTestRunner.class)
public class PageTest {
    private static final WikiSite WIKI = WikiSite.forLanguageCode("en");

    @Test
    public void testMediaWikiMarshalling() throws Throwable {
        PageTitle title = new PageTitle("Main page", WIKI, "//foo/thumb.jpg");
        PageProperties props = new PageProperties(new JSONObject(BasePageLeadTest.getEnglishMainPageJson()));

        Page page = new Page(title, new ArrayList<Section>(), props, Page.MEDIAWIKI_ORIGIN);
        assertThat(page.isFromRestBase(), is(false));
    }

    @Test
    public void testRestBaseMarshalling() throws Throwable {
        PageTitle title = new PageTitle("Main page", WIKI, "//foo/thumb.jpg");
        PageProperties props = new PageProperties(new JSONObject(BasePageLeadTest.getEnglishMainPageJson()));

        Page page = new Page(title, new ArrayList<Section>(), props, Page.RESTBASE_ORIGIN);
        assertThat(page.isFromRestBase(), is(true));
    }
}
