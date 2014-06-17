package org.wikipedia.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import org.mediawiki.api.json.Api;
import org.wikipedia.WikipediaApp;
import org.wikipedia.zero.WikipediaZeroTask;
import org.wikipedia.zero.ZeroMessage;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WikipediaZeroTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;

    public WikipediaZeroTests() {
        super(TestDummyActivity.class);
    }

    public void testWikipediaZeroEligibilityCheck() throws Throwable {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        startActivity(new Intent(), null, null);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                HashMap<String, String> customHeaders = new HashMap<>();
                customHeaders.put("X-CS", "TEST");
                new WikipediaZeroTask(new Api("en.m.wikipedia.org", "WMF-Android-AutomationTest-testWikipediaZeroEligibility", customHeaders), (WikipediaApp)getInstrumentation().getTargetContext().getApplicationContext()) {
                    @Override
                    public void onFinish(ZeroMessage result) {
                        assertNotNull(result);
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
