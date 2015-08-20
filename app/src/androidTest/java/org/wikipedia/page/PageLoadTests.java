package org.wikipedia.page;

import org.wikipedia.Site;
import org.wikipedia.history.HistoryEntry;

import android.test.ActivityInstrumentationTestCase2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test loading of pages on a high level. Replacement for SectionFetchTaskTests.
 */
public class PageLoadTests extends ActivityInstrumentationTestCase2<PageActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);
    private static final Site SITE = new Site("test.wikipedia.org");
    private PageActivity activity;
    private CountDownLatch completionLatch;
    private PageFragment fragment;

    private PageLoadCallbacks callback = new PageLoadCallbacks() {
        @Override
        public void onLoadComplete() {
            completionLatch.countDown();
        }
    };

    public PageLoadTests() {
        super(PageActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();
    }

    public void testPageFetch() throws Throwable {
//        final int expectedNumberOfSections = 4;
        // TODO: verify num sections
        loadPage("Test_page_for_app_testing/Section1");
    }

    /** Inspired by https://bugzilla.wikimedia.org/show_bug.cgi?id=66152 */
    public void testPageFetchWithAmpersand() throws Throwable {
//        final int expectedNumberOfSections = 1;
        // TODO: verify num sections
        loadPage("Ampersand & title");
    }

    private void loadPage(final String title) throws Throwable {
        completionLatch = new CountDownLatch(1);
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragment = (PageFragment) activity.getTopFragment();
                fragment.setPageLoadCallbacks(callback);
                loadPage(fragment, title);
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    public static void loadPage(PageFragment fragment, String title) {
        PageTitle pageTitle = new PageTitle(null, title, SITE);
        fragment.displayNewPage(pageTitle,
                new HistoryEntry(pageTitle, HistoryEntry.SOURCE_RANDOM),
                false,
                false);
    }
}
