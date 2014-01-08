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

    private Api enwikiAPI;
    private Site enwiki;

    public PageFetchTaskTests() {
        super(TestDummyActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        enwiki = new Site("en.wikipedia.org");
        enwikiAPI = new Api("en.wikipedia.org");

        startActivity(new Intent(), null, null);
    }

    public void testPageFetch() throws Throwable {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                new SectionsFetchTask(enwikiAPI, new PageTitle(null, "India", enwiki), "all") {
                    @Override
                    public void onFinish(List<Section> result) {
                        assertNotNull(result);
                        // FIXME: SUPER FLAKY TEST BELOW! 16 is count of first level sections + 1 in en:India article!
                        assertEquals(16, result.size());
                        assertEquals(4, result.get(2).getSubSections().size());
                        assertEquals(result.get(1), Section.findSectionForID(result, 1));
                        assertEquals(result.get(2).getSubSections().get(0), Section.findSectionForID(result, 3));
                        assertEquals(result.get(result.size() - 1), Section.findSectionForID(result, 28));
                        assertEquals(result.get(10).getSubSections().get(6), Section.findSectionForID(result, 23));
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
