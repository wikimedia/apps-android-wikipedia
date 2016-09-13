package org.wikipedia.test;

import android.support.annotation.StringRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mediawiki.api.json.Api;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.editing.EditTokenStorage;
import org.wikipedia.login.LoginResult;
import org.wikipedia.login.LoginTask;
import org.wikipedia.testlib.TestLatch;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static android.support.test.InstrumentationRegistry.getInstrumentation;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class LoginTaskTest {
    private static final String TEST_WIKI = "test.wikipedia.org";
    private static final String USERNAME = getString(R.string.test_username);
    private static final String PASSWORD = getString(R.string.test_password);
    private final TestLatch completionLatch = new TestLatch();

    @Test
    public void testLogin() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                new LoginTask(new Api(TEST_WIKI), USERNAME, PASSWORD) {
                    @Override
                    public void onFinish(LoginResult result) {
                        super.onFinish(result);
                        completionLatch.countDown();
                        assertThat(result.getStatus(), equalTo("PASS"));
                        WikipediaApp.getInstance().getEditTokenStorage().get(new Site(TEST_WIKI), callback);
                    }
                }.execute();
            }
        });
        completionLatch.await();
    }

    private EditTokenStorage.TokenRetrievedCallback callback = new EditTokenStorage.TokenRetrievedCallback() {
        @Override
        public void onTokenRetrieved(String token) {
            assertThat(token.equals("+\\"), is(false));
            completionLatch.countDown();
        }

        @Override
        public void onTokenFailed(Throwable caught) {
            throw new RuntimeException(caught);
        }
    };

    private static String getString(@StringRes int id) {
        return getInstrumentation().getContext().getString(id);
    }
}