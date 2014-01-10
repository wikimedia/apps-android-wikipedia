
package org.wikipedia.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.editing.DoEditTask;
import org.wikipedia.editing.FetchSectionWikitextTask;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DoEditTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;

    public DoEditTaskTests() {
        super(TestDummyActivity.class);
    }

    public void testPageFetch() throws Throwable {
        startActivity(new Intent(), null, null);
        final PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Section1", new Site("test.wikipedia.org"));
        final String wikitext = "== Section 2 ==\n\nEditing section test at " + System.currentTimeMillis();
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                new DoEditTask(getInstrumentation().getTargetContext(), title, wikitext, 3) {
                    @Override
                    public void onFinish(String result) {
                        assertNotNull(result);
                        assertEquals("Success", result);
                        new FetchSectionWikitextTask(getInstrumentation().getTargetContext(),title, 3) {
                            @Override
                            public void onFinish(String result) {
                                assertNotNull(result);
                                assertEquals(wikitext, result);
                                completionLatch.countDown();
                            }
                        }.execute();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}

