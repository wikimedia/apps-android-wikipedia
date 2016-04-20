package org.wikipedia.test;

import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.createaccount.CompatCreateAccountResult;
import org.wikipedia.createaccount.CreateAccountCaptchaResult;
import org.wikipedia.createaccount.authmanager.AMCreateAccountResult;
import org.wikipedia.createaccount.authmanager.AMCreateAccountSuccessResult;
import org.wikipedia.createaccount.authmanager.AMCreateAccountTask;
import org.wikipedia.createaccount.CreateAccountResult;
import org.wikipedia.createaccount.CreateAccountSuccessResult;
import org.wikipedia.createaccount.CreateAccountTask;
import org.wikipedia.editing.CaptchaResult;
import org.wikipedia.login.authmanager.AMLoginInfoResult;
import org.wikipedia.login.authmanager.AMLoginInfoTask;
import org.wikipedia.testlib.TestLatch;
import org.wikipedia.util.log.L;

import static junit.framework.Assert.fail;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@RunWith(AndroidJUnit4.class)
public class CreateAccountTaskTest {
    private static Site TEST_WIKI = new Site("test.wikipedia.org");
    private boolean isAuthManagerEnabled = false;

    private LoginInfo loginInfo = new LoginInfo() {
        @Override
        public void onCatch(Throwable caught) {
            L.e(caught);
            fail();
        }

        @Override
        public void onFinish(AMLoginInfoResult result) {
            super.onFinish(result);
            if (result.getEnabled()) {
                L.i("Logging in with AuthManager");
                isAuthManagerEnabled = true;
            } else {
                L.i("Logging in with legacy login");
            }
        }
    };

    @Test
    public void testTokenFetch() {
        long millis = System.currentTimeMillis();
        CompatCreateAccountResult result;

        final String username = "someusername" + millis;
        final String password = "somepassword" + millis;

        loginInfo.execute();
        loginInfo.await();

        if (isAuthManagerEnabled) {
            AMSubject amSubject = new AMSubject(username, password, password, null);
            amSubject.execute();
            result = amSubject.await();
        } else {
            Subject subject = new Subject(username, password, null);
            subject.execute();
            result = subject.await();
        }
        handleCreateAccountResult(result);
    }

    private void handleCreateAccountResult(CompatCreateAccountResult result) {
        // We don't always get a CAPTCHA when running this test repeatedly
        if (result instanceof AMCreateAccountSuccessResult
                || result instanceof CreateAccountSuccessResult) {
            return;
        }

        // If we made it here, we did get a CAPTCHA (this won't happen at this stage with AM)
        assertThat(result, instanceOf(CreateAccountCaptchaResult.class));
        CaptchaResult captchaResult = ((CreateAccountCaptchaResult) result).getCaptchaResult();
        assertThat(captchaResult, notNullValue());
        assertThat(captchaResult.getCaptchaId(), not(isEmptyOrNullString()));

        String expectedCaptchaUrlPrefix = WikipediaApp.getInstance().getSite().scheme()
                + "://test.wikipedia.org/w/index.php?title=Special:Captcha/image";
        assertThat(captchaResult.getCaptchaUrl(TEST_WIKI), startsWith(expectedCaptchaUrlPrefix));
    }

    private static class LoginInfo extends AMLoginInfoTask {
        @NonNull private final TestLatch latch = new TestLatch();
        private AMLoginInfoResult result;

        @Override
        public void onFinish(AMLoginInfoResult result) {
            super.onFinish(result);
            this.result = result;
            latch.countDown();
        }

        public AMLoginInfoResult await() {
            latch.await();
            return result;
        }
    }

    private static class AMSubject extends AMCreateAccountTask {
        @NonNull private final TestLatch latch = new TestLatch();
        private CompatCreateAccountResult result;

        AMSubject(String username, String password, String repeat, String email) {
            super(username, password, repeat, email);
        }

        @Override
        public void onFinish(AMCreateAccountResult result) {
            super.onFinish(result);
            this.result = result;
            latch.countDown();
        }

        public CompatCreateAccountResult await() {
            latch.await();
            return result;
        }
    }

    private static class Subject extends CreateAccountTask {
        @NonNull private final TestLatch latch = new TestLatch();
        private CompatCreateAccountResult result;

        Subject(String username, String password, String email) {
            super(getTargetContext(), username, password, email);
        }

        @Override
        public void onFinish(CreateAccountResult result) {
            super.onFinish(result);
            this.result = result;
            latch.countDown();
        }

        public CompatCreateAccountResult await() {
            latch.await();
            return result;
        }
    }
}
