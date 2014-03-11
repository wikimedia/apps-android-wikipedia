
package org.wikipedia.test;

import android.content.*;
import android.test.*;
import org.wikipedia.*;
import org.wikipedia.editing.*;

import java.util.concurrent.*;

public class TriggerEditCaptchaTest extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;

    public TriggerEditCaptchaTest() {
        super(TestDummyActivity.class);
    }

    public void testCaptchaTrigger() throws Throwable {
        startActivity(new Intent(), null, null);
        final PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Captcha", new Site("test.wikipedia.org"));
        final String wikitext = "== Section 2 ==\n\nEditing by inserting an external link https://" + System.currentTimeMillis();
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                new DoEditTask(getInstrumentation().getTargetContext(), title, wikitext, 0, "+\\", "") {
                    @Override
                    public void onFinish(EditingResult result) {
                        assertNotNull(result);
                        assertTrue(result instanceof CaptchaResult);
                        CaptchaResult captchaResult = (CaptchaResult) result;
                        String captchaUrl = captchaResult.getCaptchaUrl(new Site("test.wikipedia.org"));
                        assertTrue(captchaUrl.startsWith(WikipediaApp.PROTOCOL + "://test.wikipedia.org/w/index.php?title=Special:Captcha/image"));
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}

