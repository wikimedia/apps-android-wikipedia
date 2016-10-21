package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.editing.EditTokenStorage;
import org.wikipedia.testlib.TestLatch;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.wikipedia.test.TestUtil.runOnMainSync;

@SmallTest
@RunWith(AndroidJUnit4.class)
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
                                WikipediaApp.getInstance().getEditTokenStorage().get(TEST_WIKI, callback);
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
