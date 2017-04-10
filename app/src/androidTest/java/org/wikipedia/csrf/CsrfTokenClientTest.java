package org.wikipedia.csrf;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import org.junit.Before;
import org.junit.Test;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.login.LoginClient;
import org.wikipedia.login.LoginResult;
import org.wikipedia.login.User;
import org.wikipedia.testlib.TestLatch;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.wikipedia.test.TestUtil.runOnMainSync;

public class CsrfTokenClientTest {
    private static final WikiSite TEST_WIKI = WikiSite.forLanguageCode("test");
    private static final String USERNAME = getString(org.wikipedia.test.R.string.test_username);
    private static final String PASSWORD = getString(org.wikipedia.test.R.string.test_password);

    @Before
    public void setUp() {
        User.disableStorage(); // don't change the app login from this test
    }

    @Test
    public void testCsrfTokenForAnon() {
        WikipediaApp.getInstance().getCookieManager().clearAllCookies();
        final TestLatch completionLatch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                getCsrfToken(completionLatch, false);
            }
        });
        completionLatch.await();
    }

    @Test
    public void testCsrfTokenForLogin() {
        WikipediaApp.getInstance().getCookieManager().clearAllCookies();
        final TestLatch completionLatch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                new LoginClient().request(TEST_WIKI, USERNAME, PASSWORD,
                        new LoginClient.LoginCallback() {
                            @Override
                            public void success(@NonNull LoginResult result) {
                                assertThat(result.getStatus(), equalTo("PASS"));
                                getCsrfToken(completionLatch, true);
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

    @Test
    public void testCsrfTokenLoginError() {
        WikipediaApp.getInstance().getCookieManager().clearAllCookies();
        final TestLatch completionLatch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                new LoginClient().request(TEST_WIKI, USERNAME, "foo",
                        new LoginClient.LoginCallback() {
                            @Override
                            public void success(@NonNull LoginResult result) {
                                assertThat("login succeeded, when it shouldn't have!", false);
                            }

                            @Override
                            public void twoFactorPrompt(@NonNull Throwable throwble, @Nullable String token) {
                                fail("Two-factor prompt not expected here");
                            }

                            @Override
                            public void error(@NonNull Throwable caught) {
                                assertThat(caught, is(instanceOf(LoginClient.LoginFailedException.class)));
                                completionLatch.countDown();
                            }
                        });
            }
        });
        completionLatch.await();
    }

    private void getCsrfToken(final TestLatch completionLatch, final boolean loggedIn) {
        new CsrfTokenClient(TEST_WIKI, TEST_WIKI).request(new CsrfTokenClient.Callback() {
            @Override
            public void success(@NonNull String token) {
                assertThat(token.equals(CsrfTokenClient.ANON_TOKEN), is(!loggedIn));
                completionLatch.countDown();
            }

            @Override
            public void failure(@NonNull Throwable caught) {
                throw new RuntimeException(caught);
            }

            @Override
            public void twoFactorPrompt() {
                throw new RuntimeException("TODO: test 2FA login.");
            }
        });
    }

    private static String getString(@StringRes int id) {
        return getInstrumentation().getContext().getString(id);
    }
}
