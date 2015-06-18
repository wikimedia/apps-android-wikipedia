package org.wikipedia.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.editing.AbuseFilterEditResult;
import org.wikipedia.editing.DoEditTask;
import org.wikipedia.editing.EditingResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TriggerAbuseFilterTest extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;

    public TriggerAbuseFilterTest() {
        super(TestDummyActivity.class);
    }

    /**
     * Test handling of abuse filter warnings which warn users about making edits of a certain kind.
     *
     * Type:   warn
     * Action: editing any userspace page while logged out
     * Filter: https://test.wikipedia.org/wiki/Special:AbuseFilter/94
     *
     * @throws Throwable
     */
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
                new DoEditTask(getInstrumentation().getTargetContext(), title, wikitext, 0, "+\\", "", false) {
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

    /**
     * Test handling of abuse filter errors which completely disallow edits of a certain kind.
     *
     * Type:   disallow
     * Action: adding string "poop" to page text
     * Filter: https://test.wikipedia.org/wiki/Special:AbuseFilter/2
     *
     * @throws Throwable
     */
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
                new DoEditTask(getInstrumentation().getTargetContext(), title, wikitext, 0, "+\\", "", false) {
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
     * Test the app's handling of the abuse filter emitting arbitrary error codes.
     *
     * Type:   warn
     * Action: adding string "appcrashtest" to page text
     * Filter: https://test.wikipedia.org/wiki/Special:AbuseFilter/152
     *
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
                new DoEditTask(getInstrumentation().getTargetContext(), title, wikitext, 0, "+\\", "", false) {
                    @Override
                    public void onFinish(EditingResult result) {
                        assertNotNull(result);
                        assertTrue(result instanceof AbuseFilterEditResult);
                        // For now we handle arbitrary error codes as TYPE_ERROR. This may change.
                        assertEquals(((AbuseFilterEditResult) result).getType(), AbuseFilterEditResult.TYPE_ERROR);
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}