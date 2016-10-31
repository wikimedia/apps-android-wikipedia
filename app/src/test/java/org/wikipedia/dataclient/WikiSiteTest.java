package org.wikipedia.dataclient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.TestRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(TestRunner.class) public class WikiSiteTest {
    @Test public void testEquals() {
        assertThat(WikiSite.forLanguageCode("en"), is(WikiSite.forLanguageCode("en")));

        assertThat(WikiSite.forLanguageCode("ta"), not(WikiSite.forLanguageCode("en")));
        assertThat(WikiSite.forLanguageCode("ta").equals("ta.wikipedia.org"), is(false));
    }

    @Test public void testNormalization() {
        assertThat("bm.wikipedia.org", is(WikiSite.forLanguageCode("bm").authority()));
    }

    @Test public void testIsSupportedSite() {
        assertThat(WikiSite.supportedAuthority("fr.wikipedia.org"), is(true));
        assertThat(WikiSite.supportedAuthority("fr.m.wikipedia.org"), is(true));
        assertThat(WikiSite.supportedAuthority("roa-rup.wikipedia.org"), is(true));

        assertThat(WikiSite.supportedAuthority("google.com"), is(false));
    }

    @Test public void testTitleForInternalLink() {
        WikiSite wiki = WikiSite.forLanguageCode("en");
        assertThat(new PageTitle("Main Page", wiki), is(wiki.titleForInternalLink("")));
        assertThat(new PageTitle("Main Page", wiki), is(wiki.titleForInternalLink("/wiki/")));
        assertThat(new PageTitle("wiki", wiki), is(wiki.titleForInternalLink("wiki")));
        assertThat(new PageTitle("wiki", wiki), is(wiki.titleForInternalLink("/wiki/wiki")));
        assertThat(new PageTitle("wiki/wiki", wiki), is(wiki.titleForInternalLink("/wiki/wiki/wiki")));
    }
}