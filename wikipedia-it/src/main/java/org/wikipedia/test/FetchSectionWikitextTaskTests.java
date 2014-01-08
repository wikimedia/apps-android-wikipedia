
package org.wikipedia.test;

import android.content.Context;
import android.content.Intent;
import android.test.ActivityUnitTestCase;
import org.mediawiki.api.json.Api;
import org.wikipedia.editing.*;
import org.wikipedia.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FetchSectionWikitextTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;

    public FetchSectionWikitextTaskTests() {
        super(TestDummyActivity.class);
    }

    public void testPageFetch() throws Throwable {
        startActivity(new Intent(), null, null);
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Section1", new Site("test.wikipedia.org"));
                new FetchSectionWikitextTask(getInstrumentation().getTargetContext(),title, 2) {
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

