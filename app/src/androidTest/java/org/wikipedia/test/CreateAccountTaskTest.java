package org.wikipedia.test;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.facebook.drawee.view.SimpleDraweeView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mediawiki.api.json.Api;
import org.wikipedia.Site;
import org.wikipedia.createaccount.CreateAccountInfoResult;
import org.wikipedia.createaccount.CreateAccountInfoTask;
import org.wikipedia.editing.CaptchaResult;
import org.wikipedia.testlib.TestLatch;
import org.wikipedia.util.log.L;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Automated tests for the account creation flow.
 *
 * Note that we won't be able to test for successful account creation directly, since the WPs are
 * configured to require a CAPTCHA for account creation and by design we won't be able to pass
 * a CAPTCHA.  The most we can do is check that we are able to retrieve the createaccount token and
 * CAPTCHA image as expected.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class CreateAccountTaskTest {
    private static Api TEST_WIKI_API = new Api("test.wikipedia.org");
    private static Site TEST_WIKI_SITE = new Site("test.wikipedia.org");
    private Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    private CreateAccountTestInfoTask createAccountInfoTask
            = new CreateAccountTestInfoTask(TEST_WIKI_API) {
        @Override
        public void onCatch(Throwable caught) {
            L.e(caught);
            fail();
        }
    };

    @Test
    public void testCreateAccountInfoFetch() {
        createAccountInfoTask.execute();
        CreateAccountInfoResult result = createAccountInfoTask.await();
        assertNotNull(result.token());
        assertTrue(result.hasCaptcha());
        assertNotNull(result.captchaId());
    }

    @Test
    public void testFetchCaptchaImage() {
        createAccountInfoTask.execute();
        CreateAccountInfoResult result = createAccountInfoTask.await();
        CaptchaResult captcha = new CaptchaResult(result.captchaId());
        SimpleDraweeView captchaView = new SimpleDraweeView(context);
        captchaView.setImageURI(Uri.parse(captcha.getCaptchaUrl(TEST_WIKI_SITE)));
        assertNotNull(captchaView.getDrawable());
    }

    private static class CreateAccountTestInfoTask extends CreateAccountInfoTask {
        @NonNull private final TestLatch latch = new TestLatch();
        private CreateAccountInfoResult result;

        CreateAccountTestInfoTask(Api api) {
            super(api);
        }

        @Override
        public void onFinish(CreateAccountInfoResult result) {
            super.onFinish(result);
            this.result = result;
            latch.countDown();
        }

        public CreateAccountInfoResult await() {
            latch.await();
            return result;
        }
    }
}
