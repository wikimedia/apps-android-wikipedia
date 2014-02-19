
package org.wikipedia.test;

import android.content.*;
import android.test.*;
import android.text.*;
import org.wikimedia.wikipedia.test.R;
import org.wikipedia.*;
import org.wikipedia.createaccount.*;
import org.wikipedia.editing.*;
import org.wikipedia.login.*;

import java.util.concurrent.*;

public class CreateAccountTokenTest extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;

    public CreateAccountTokenTest() {
        super(TestDummyActivity.class);
    }

    public void testTokenFetch() throws Throwable {
        startActivity(new Intent(), null, null);
        final Site testWiki = new Site("test.wikipedia.org");
        final String username = "someusername" + System.currentTimeMillis();
        final String password = "somepassword" + System.currentTimeMillis();
        final WikipediaApp app = (WikipediaApp)getInstrumentation().getTargetContext().getApplicationContext();

        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                new CreateAccountTokenFetchTask(getInstrumentation().getTargetContext(), username, password) {
                    @Override
                    public void onFinish(CreateAccountTokenFetchTask.CreateAccountTokenResult result) {
                        assertNotNull(result);
                        assertNotNull(result.getCaptchaResult());
                        assertFalse(TextUtils.isEmpty(result.getToken()));
                        assertFalse(TextUtils.isEmpty(result.getCaptchaResult().getCaptchaId()));
                        String captchaUrl = result.getCaptchaResult().getCaptchaUrl(testWiki);
                        assertTrue(captchaUrl.startsWith(WikipediaApp.PROTOCOL + "://test.wikipedia.org/w/index.php?title=Special:Captcha/image"));
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}

