package org.wikipedia.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.editing.AbuseFilterEditResult;
import org.wikipedia.editing.DoEditTask;
import org.wikipedia.editing.EditingResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TriggerAbuseFilterTest extends ActivityUnitTestCase<TestDummyActivity> {
    // TODO: Document the tests in this class.
    // TODO: Add links to filters that are triggered so you can check for changes if the test fails.
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

    /**
     * Test this filter: https://test.wikipedia.org/wiki/Special:AbuseFilter/152
     * TODO: Improve this documentation so that it doesn't just make sense to the method's author.
     * @throws Throwable
     */
    public void testAbuseFilterTriggerStopOnArbitraryErrorCode() throws Throwable {
        startActivity(new Intent(), null, null);
        final PageTitle title = new PageTitle(null, "Test_page_for_app_testing/AbuseFilter", new Site("test.wikipedia.org"));
        final String wikitext = "== Section 2 ==\n\nTriggering AbuseFilter number 152 by saying appcrashtest many times at " + System.currentTimeMillis();
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