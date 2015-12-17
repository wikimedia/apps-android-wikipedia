package org.wikipedia.page;

import org.junit.Test;

public class PageLoadTest extends BasePageLoadTest {
    @Test
    public void testPageFetch() throws Throwable {
//        final int expectedNumberOfSections = 4;
        // TODO: verify num sections
        loadPage("Test_page_for_app_testing/Section1");
    }

    /** Inspired by https://bugzilla.wikimedia.org/show_bug.cgi?id=66152 */
    @Test
    public void testPageFetchWithAmpersand() throws Throwable {
//        final int expectedNumberOfSections = 1;
        // TODO: verify num sections
        loadPage("Ampersand & title");
    }
}