package org.wikipedia.test;

import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.createaccount.CreateAccountCaptchaResult;
import org.wikipedia.createaccount.CreateAccountResult;
import org.wikipedia.createaccount.CreateAccountSuccessResult;
import org.wikipedia.createaccount.CreateAccountTask;
import org.wikipedia.editing.CaptchaResult;
import org.wikipedia.testlib.TestLatch;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@RunWith(AndroidJUnit4.class)
public class CreateAccountTaskTest {
    @Test
    public void testTokenFetch() {
        Site testWiki = new Site("test.wikipedia.org");
        String username = "someusername" + System.currentTimeMillis();
        String password = "somepassword" + System.currentTimeMillis();

        Subject subject = new Subject(username, password, null);
        subject.execute();
        CreateAccountResult result = subject.await();

        if (!(result instanceof CreateAccountSuccessResult)) {
            // We don't always get a CAPTCHA when running this test repeatedly

            assertThat(result, instanceOf(CreateAccountCaptchaResult.class));
            CaptchaResult captchaResult = ((CreateAccountCaptchaResult) result).getCaptchaResult();
            assertThat(captchaResult, notNullValue());
            assertThat(captchaResult.getCaptchaId(), not(isEmptyOrNullString()));

            String expectedCaptchaUrlPrefix = WikipediaApp.getInstance().getNetworkProtocol()
                    + "://test.wikipedia.org/w/index.php?title=Special:Captcha/image";
            assertThat(captchaResult.getCaptchaUrl(testWiki), startsWith(expectedCaptchaUrlPrefix));
        }
    }

    private static class Subject extends CreateAccountTask {
        @NonNull private final TestLatch latch = new TestLatch();
        private CreateAccountResult result;

        Subject(String username, String password, String email) {
            super(getTargetContext(), username, password, email);
        }

        @Override
        public void onFinish(CreateAccountResult result) {
            super.onFinish(result);
            this.result = result;
            latch.countDown();
        }

        public CreateAccountResult await() {
            latch.await();
            return result;
        }
    }
}