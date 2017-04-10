package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import org.junit.Before;
import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.testlib.TestLatch;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.wikipedia.test.TestUtil.runOnMainSync;

public class LoginClientTest {
    private static final WikiSite TEST_WIKI = WikiSite.forLanguageCode("test");
    private static final String USERNAME = getString(org.wikipedia.test.R.string.test_username);
    private static final String PASSWORD = getString(org.wikipedia.test.R.string.test_password);
    private final TestLatch completionLatch = new TestLatch();

    @Before
    public void setUp() {
        User.disableStorage(); // don't change the app login from this test
    }

    @Test
    public void testLogin() {
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                new LoginClient().request(TEST_WIKI, USERNAME, PASSWORD,
                        new LoginClient.LoginCallback() {
                            @Override
                            public void success(@NonNull LoginResult result) {
                                completionLatch.countDown();
                                assertThat(result.getStatus(), equalTo("PASS"));
                            }

                            @Override
                            public void twoFactorPrompt(@NonNull Throwable throwble, @Nullable String token) {
                                fail("Two-factor prompt not expected here");
                            }

                            @Override
                            public void error(@NonNull Throwable caught) {
                                assertThat("login failed!", false);
                            }
                        });
            }
        });
        completionLatch.await();
    }

    private static String getString(@StringRes int id) {
        return getInstrumentation().getContext().getString(id);
    }
}
