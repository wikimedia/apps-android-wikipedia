
package org.wikipedia.test;

import android.support.test.filters.SmallTest;
import android.test.ActivityUnitTestCase;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.editing.FetchSectionWikitextTask;
import org.wikipedia.page.PageTitle;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SmallTest
public class FetchSectionWikitextTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20_000;

    public FetchSectionWikitextTaskTests() {
        super(TestDummyActivity.class);
    }

    public void testPageFetch() throws Throwable {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Section1", WikiSite.forLanguageCode("test"));
                new FetchSectionWikitextTask(getInstrumentation().getTargetContext(), title, 2) {
                    @Override
                    public void onFinish(String result) {
                        assertNotNull(result);
                        assertEquals(result, "=== Section1.2 ===\nThis is a subsection");
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}

