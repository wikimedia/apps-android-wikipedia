package org.wikipedia.page;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.WikiSite;

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
        PageProperties props = new PageProperties(title, true);

        Page page = new Page(title, new ArrayList<>(), props, Page.MEDIAWIKI_ORIGIN);
        assertThat(page.isFromRestBase(), is(false));
    }

    @Test
    public void testRestBaseMarshalling() throws Throwable {
        PageTitle title = new PageTitle("Main page", WIKI, "//foo/thumb.jpg");
        PageProperties props = new PageProperties(title, true);

        Page page = new Page(title, new ArrayList<>(), props, Page.RESTBASE_ORIGIN);
        assertThat(page.isFromRestBase(), is(true));
    }
}
