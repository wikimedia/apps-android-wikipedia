package org.wikipedia.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.text.TextUtils;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.createaccount.CreateAccountCaptchaResult;
import org.wikipedia.createaccount.CreateAccountResult;
import org.wikipedia.createaccount.CreateAccountSuccessResult;
import org.wikipedia.createaccount.CreateAccountTask;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                new CreateAccountTask(getInstrumentation().getTargetContext(), username, password, null) {
                    @Override
                    public void onFinish(CreateAccountResult baseResult) {
                        if (baseResult instanceof CreateAccountSuccessResult) {
                            // We don't always get a CAPTCHA when running this test repeatedly
                            completionLatch.countDown();
                            return;
                        }
                        assertTrue("got " + baseResult.getClass().getSimpleName(),
                                baseResult instanceof CreateAccountCaptchaResult);
                        CreateAccountCaptchaResult result = (CreateAccountCaptchaResult)baseResult;
                        assertNotNull(result);
                        assertNotNull(result.getCaptchaResult());
                        assertFalse(TextUtils.isEmpty(result.getCaptchaResult().getCaptchaId()));
                        String captchaUrl = result.getCaptchaResult().getCaptchaUrl(testWiki);
                        assertTrue(captchaUrl.startsWith(WikipediaApp.getInstance().getNetworkProtocol()
                                + "://test.wikipedia.org/w/index.php?title=Special:Captcha/image"));
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}

