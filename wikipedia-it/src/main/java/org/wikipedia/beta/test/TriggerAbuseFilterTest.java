
package org.wikipedia.beta.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import org.wikipedia.beta.PageTitle;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.editing.AbuseFilterEditResult;
import org.wikipedia.beta.editing.DoEditTask;
import org.wikipedia.beta.editing.EditingResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TriggerAbuseFilterTest extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;

    public TriggerAbuseFilterTest() {
        super(TestDummyActivity.class);
    }

    public void testAbuseFilterTriggerWarn() throws Throwable {
        startActivity(new Intent(), null, null);
        final PageTitle title = new PageTitle(null, "User:Yuvipandaaaaaaaa", new Site("test.wikipedia.org"));
        final String wikitext = "Testing Abusefilter by simply editing this page. Triggering rule 94 at " + System.currentTimeMillis();
        final WikipediaApp app = (WikipediaApp) getInstrumentation().getTargetContext().getApplicationContext();
        app.getEditTokenStorage().clearAllTokens();
        app.getCookieManager().clearAllCookies();
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                new DoEditTask(getInstrumentation().getTargetContext(), title, wikitext, 0, "+\\", "") {
                    @Override
                    public void onFinish(EditingResult result) {
                        assertNotNull(result);
                        assertTrue(result instanceof AbuseFilterEditResult);
                        assertEquals(((AbuseFilterEditResult) result).getType(), AbuseFilterEditResult.TYPE_WARNING);
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    public void testAbuseFilterTriggerStop() throws Throwable {
        startActivity(new Intent(), null, null);
        final PageTitle title = new PageTitle(null, "Test_page_for_app_testing/AbuseFilter", new Site("test.wikipedia.org"));
        final String wikitext = "== Section 2 ==\n\nTriggering AbuseFilter number 2 by saying poop many times at " + System.currentTimeMillis();
        final WikipediaApp app = (WikipediaApp) getInstrumentation().getTargetContext().getApplicationContext();
        app.getEditTokenStorage().clearAllTokens();
        app.getCookieManager().clearAllCookies();
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                new DoEditTask(getInstrumentation().getTargetContext(), title, wikitext, 0, "+\\", "") {
                    @Override
                    public void onFinish(EditingResult result) {
                        assertNotNull(result);
                        assertTrue(result instanceof AbuseFilterEditResult);
                        assertEquals(((AbuseFilterEditResult) result).getType(), AbuseFilterEditResult.TYPE_ERROR);
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
