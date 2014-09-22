
package org.wikipedia.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.editing.EditPreviewTask;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PreviewTaskTests extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;

    public PreviewTaskTests() {
        super(TestDummyActivity.class);
    }

    public void testPreview() throws Throwable {
        startActivity(new Intent(), null, null);
        final PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Section1", new Site("test.wikipedia.org"));
        long randomTime = System.currentTimeMillis();
        final String wikitext = "== Section 2 ==\n\nEditing section INSERT RANDOM & HERE test at " + randomTime;
        final String expected = "<div></div><h2><span class=\"mw-headline\" id=\"Section_2\">Section 2</span><a href=\"#/editor/1\" data-section=\"1\" class=\"edit-page icon icon-32px icon-edit enabled\">Edit</a></h2><div>\n<p>Editing section INSERT RANDOM &amp; HERE test at " + randomTime + "</p>\n\n\n\n</div>";
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                new EditPreviewTask(getInstrumentation().getTargetContext(), wikitext, title) {
                    @Override
                    public void onFinish(String result) {
                        assertNotNull(result);
                        assertEquals(result, expected);
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
