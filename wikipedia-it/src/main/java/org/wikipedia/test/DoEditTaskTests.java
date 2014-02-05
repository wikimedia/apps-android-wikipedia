
package org.wikipedia.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.editing.DoEditTask;
import org.wikipedia.editing.EditingResult;
import org.wikipedia.editing.FetchSectionWikitextTask;
import org.wikipedia.editing.EditTokenStorage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DoEditTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;

    public DoEditTaskTests() {
        super(TestDummyActivity.class);
    }

    public void testEdit() throws Throwable {
        startActivity(new Intent(), null, null);
        final PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Section1", new Site("test.wikipedia.org"));
        final String wikitext = "== Section 2 ==\n\nEditing section INSERT RANDOM & HERE test at " + System.currentTimeMillis();
        final WikipediaApp app = (WikipediaApp)getInstrumentation().getTargetContext().getApplicationContext();
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                app.getEditTokenStorage().get(title.getSite(), new EditTokenStorage.TokenRetreivedCallback() {
                    @Override
                    public void onTokenRetreived(String token) {
                        new DoEditTask(getInstrumentation().getTargetContext(), title, wikitext, 3, token) {
                            @Override
                            public void onFinish(EditingResult result) {
                                assertNotNull(result);
                                assertEquals("Success", result.getResult());
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
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}

