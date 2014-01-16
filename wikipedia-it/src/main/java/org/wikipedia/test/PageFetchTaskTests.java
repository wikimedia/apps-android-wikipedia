package org.wikipedia.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import org.mediawiki.api.json.Api;
import org.wikipedia.*;
import org.wikipedia.page.Section;
import org.wikipedia.page.SectionsFetchTask;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PageFetchTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;

    public PageFetchTaskTests() {
        super(TestDummyActivity.class);
    }

    public void testPageFetch() throws Throwable {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        startActivity(new Intent(), null, null);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                new SectionsFetchTask(getInstrumentation().getTargetContext(), new PageTitle(null, "Test_page_for_app_testing/Section1", new Site("test.wikipedia.org")), "all") {
                    @Override
                    public void onFinish(List<Section> result) {
                        assertNotNull(result);
                        assertEquals(4, result.size());
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
